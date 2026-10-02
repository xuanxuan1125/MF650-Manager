"""Index saved 8081 sources without making network requests or running JavaScript."""

import argparse
import hashlib
import json
import re
from html.parser import HTMLParser
from pathlib import Path


class InlineBlocks(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=False)
        self.blocks = []
        self.active = None

    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style") and not dict(attrs).get("src"):
            self.active = {"tag": tag, "line": self.getpos()[0], "parts": []}

    def handle_data(self, data):
        if self.active is not None:
            self.active["parts"].append(data)

    def handle_endtag(self, tag):
        if self.active is not None and tag == self.active["tag"]:
            self.blocks.append(self.active)
            self.active = None


def call_arguments(text, start):
    """Balance a fetch argument list, ignoring strings and comments; do not evaluate it."""
    depth, quote, escaped, comment = 1, None, False, None
    i = start
    while i < len(text):
        char, pair = text[i], text[i : i + 2]
        if comment == "line":
            if char == "\n":
                comment = None
        elif comment == "block":
            if pair == "*/":
                comment = None
                i += 1
        elif quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
        elif char in "'\"`":
            quote = char
        elif pair in ("//", "/*"):
            comment = "line" if pair == "//" else "block"
            i += 1
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[start:i]
        i += 1
    raise ValueError("Unclosed fetch call")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="Directory containing saved HTML/JS")
    parser.add_argument("output", type=Path, help="Local analysis directory")
    args = parser.parse_args()
    for folder in ("js", "css", "reports"):
        (args.output / folder).mkdir(parents=True, exist_ok=True)
    sources, calls, literals, other_calls = [], [], [], []
    for path in sorted(args.source.iterdir()):
        if path.suffix.lower() not in (".html", ".js"):
            continue
        data = path.read_bytes()
        text = data.decode("utf-8-sig", errors="replace").replace("\r\n", "\n")
        sources.append({"file": path.name, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
        if path.suffix.lower() == ".html":
            blocks = InlineBlocks()
            blocks.feed(text)
            for index, block in enumerate(blocks.blocks, 1):
                suffix = ".js" if block["tag"] == "script" else ".css"
                folder = "js" if suffix == ".js" else "css"
                output = args.output / folder / f"{path.stem}-{index}{suffix}"
                output.write_text("".join(block["parts"]), encoding="utf-8", newline="\n")
                sources[-1].setdefault("inline_blocks", []).append({"file": str(output.relative_to(args.output)), "source_line": block["line"]})
        for match in re.finditer(r"\bfetch\s*\(", text):
            code = call_arguments(text, match.end())
            literal = re.match(r"\s*(['\"`])(.*?)\1", code, re.S)
            method = re.search(r"\bmethod\s*:\s*(['\"])([A-Z]+)\1", code)
            calls.append({"file": path.name, "line": text.count("\n", 0, match.start()) + 1,
                          "url_literal": literal[2] if literal else None,
                          "method": method[2] if method else ("UNKNOWN" if re.search(r"\bmethod\s*:", code) else "GET"),
                          "arguments": code})
        for match in re.finditer(r"['\"`](/api/[^'\"`\s]+)", text):
            literals.append({"file": path.name, "line": text.count("\n", 0, match.start()) + 1, "literal": match[1]})
        for match in re.finditer(r"\b(?:callApi|postApi|getApi|handleUpload)\s*\(|\bxhr\.open\s*\(", text):
            code = call_arguments(text, match.end())
            if "/api/" in code or "API_URL" in code:
                other_calls.append({"file": path.name, "line": text.count("\n", 0, match.start()) + 1,
                                    "caller": match.group(0).split("(")[0].strip(), "arguments": code})
    result = {"scope": "Saved sources only; lexical index requires manual review of dynamic URLs/methods and comments.",
              "sources": sources, "fetch_calls": calls, "api_literals": literals, "wrapper_and_xhr_calls": other_calls}
    (args.output / "reports/source-index.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"sources": len(sources), "fetch_calls": len(calls), "api_literals": len(literals),
                      "dynamic_fetch_calls": sum(c["url_literal"] is None or "${" in c["url_literal"] or c["method"] == "UNKNOWN" for c in calls),
                      "wrapper_and_xhr_calls": len(other_calls)}))


if __name__ == "__main__":
    main()
