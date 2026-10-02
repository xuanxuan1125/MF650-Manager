"""Static ARM ELF inspection only; never loads or executes firmware programs.

Requires capstone and pyelftools. Emits Thumb text and ARM PLT disassembly,
literal-pool values, PC-relative string candidates and ELF relocation maps.
Candidate comments assist manual review; they are not a decompiler or proof of
reachability. GNU ARM PLT layout is checked before labelling its entries.
"""
import argparse
import json
from pathlib import Path
import struct

from capstone import Cs, CS_ARCH_ARM, CS_MODE_ARM, CS_MODE_THUMB
from capstone.arm import ARM_OP_IMM, ARM_OP_MEM, ARM_OP_REG, ARM_REG_PC
from elftools.elf.elffile import ELFFile


def inspect(path, output):
    with path.open("rb") as source:
        elf = ELFFile(source)
        if elf.header.e_machine != "EM_ARM" or not elf.little_endian:
            raise ValueError("this helper handles little-endian ARM ELF only")
        sections = [(s.header.sh_addr, s.data()) for s in elf.iter_sections()
                    if s.header.sh_flags & 2 and s.header.sh_type != "SHT_NOBITS"]

        def read(address, length):
            for base, data in sections:
                offset = address - base
                if 0 <= offset and offset + length <= len(data):
                    return data[offset:offset + length]
            return b""

        def string_at(address):
            data = read(address, 1)
            if not data or data[0] < 32 or data[0] > 126:
                return ""
            result = bytearray()
            for offset in range(160):
                byte = read(address + offset, 1)
                if not byte:
                    return ""
                if byte == b"\0":
                    return result.decode("ascii") if len(result) >= 3 else ""
                if byte[0] not in (9, 10, 13) and not 32 <= byte[0] <= 126:
                    return ""
                result.extend(byte)
            return ""

        labels = {}
        relocations = {}
        for section in elf.iter_sections():
            if section.header.sh_type in ("SHT_DYNSYM", "SHT_SYMTAB"):
                for symbol in section.iter_symbols():
                    if symbol.entry.st_value and symbol.name:
                        labels[symbol.entry.st_value & ~1] = symbol.name
            if section.header.sh_type == "SHT_REL":
                symbols = elf.get_section(section.header.sh_link)
                for rel in section.iter_relocations():
                    symbol = symbols.get_symbol(rel.entry.r_info_sym)
                    relocations[rel.entry.r_offset] = symbol.name
        plt = elf.get_section_by_name(".plt")
        relplt = elf.get_section_by_name(".rel.plt")
        if plt and relplt:
            entries = list(relplt.iter_relocations())
            if plt.header.sh_size != 20 + 12 * len(entries):
                raise ValueError("unexpected GNU ARM PLT layout; labels would be unreliable")
            symbols = elf.get_section(relplt.header.sh_link)
            for index, rel in enumerate(entries):
                labels[plt.header.sh_addr + 20 + index * 12] = symbols.get_symbol(rel.entry.r_info_sym).name + "@plt"
        lines = []
        candidates = []
        for name, mode in ((".plt", CS_MODE_ARM), (".text", CS_MODE_THUMB)):
            section = elf.get_section_by_name(name)
            if not section:
                continue
            decoder = Cs(CS_ARCH_ARM, mode)
            decoder.detail = True
            decoder.skipdata = True
            literals = {}
            lines.append(f"\nSECTION {name}; mode={'ARM' if mode == CS_MODE_ARM else 'Thumb'}")
            for ins in decoder.disasm(section.data(), section.header.sh_addr):
                if ins.address in labels:
                    lines.append(f"\n{labels[ins.address]}:")
                comment = []
                if ins.id:
                    operands = ins.operands
                    if ins.mnemonic.startswith("ldr") and len(operands) == 2 and operands[1].type == ARM_OP_MEM and operands[1].mem.base == ARM_REG_PC:
                        slot = ((ins.address + (4 if mode == CS_MODE_THUMB else 8)) & ~3) + operands[1].mem.disp
                        raw = read(slot, 4)
                        if raw:
                            value = struct.unpack("<I", raw)[0]
                            literals[operands[0].reg] = value
                            comment.append(f"literal@0x{slot:x}=0x{value:x}")
                    if ins.mnemonic.startswith("add") and operands and operands[0].type == ARM_OP_REG and any(o.type == ARM_OP_REG and o.reg == ARM_REG_PC for o in operands[1:]):
                        register = operands[0].reg
                        if register in literals:
                            value = (literals[register] + ins.address + (4 if mode == CS_MODE_THUMB else 8)) & 0xffffffff
                            text = string_at(value)
                            comment.append(f"PC-relative candidate=0x{value:x}")
                            if text:
                                comment.append(repr(text))
                                candidates.append({"instruction": hex(ins.address), "address": hex(value), "text": text})
                    if ins.mnemonic in ("bl", "blx", "b", "b.w") and operands and operands[0].type == ARM_OP_IMM:
                        target = operands[0].imm & ~1
                        if target in labels:
                            comment.append(labels[target])
                lines.append(f"{ins.address:08x}  {ins.bytes.hex():10s} {ins.mnemonic:8s} {ins.op_str}" + (" ; " + "; ".join(comment) if comment else ""))
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text("\n".join(lines) + "\n", encoding="utf-8")
        output.with_suffix(".map.json").write_text(json.dumps({"labels": {hex(k):v for k,v in labels.items()},
            "relocations": {hex(k):v for k,v in relocations.items()}, "string_candidates": candidates}, indent=2), encoding="utf-8")
        print(f"{path.name}: {len(lines)} disassembly lines, {len(candidates)} candidate string references")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("elf", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    inspect(args.elf, args.output)
