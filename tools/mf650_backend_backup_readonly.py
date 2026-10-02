"""Print an offline backup plan. Execution is blocked until access is verified.

No subprocess, socket, HTTP, ADB invocation, device tar or target code execution.
Only the vendor BAT's /www read scope is supported; it does not identify a daemon.
"""
import argparse
import json


def plan():
    return {
        'mode': 'DRY_RUN',
        'execution_enabled': False,
        'proposed_read': {'protocol': 'ADB SYNC pull', 'remote_path': '/www',
                          'pc_destination': 'test-results/backend-backup/<run-id>/www'},
        'source': 'vendor backup BAT line 18',
        'device_shell_commands': [], 'device_write_commands': [],
        'requires_device_temp_tar': False,
        'current_backend_export_coverage': 'UNKNOWN',
        'prerequisites': ['verified existing read-only transport and file access',
                          'trusted host client; explicit single-device selection',
                          'actual listener executable/path and backup coverage evidence',
                          'separate authorization for the future execution phase'],
        'stop_conditions': ['transport/auth/permission failure', 'need to restart or switch USB',
                            'need to upload or create any device file', 'unverified backend path'],
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dry-run', action='store_true', help='default; prints the plan only')
    parser.parse_args()
    print(json.dumps(plan(), ensure_ascii=False, indent=2))
