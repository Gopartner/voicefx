import re, json, sys

UUID_PATTERN = r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

def extract_first_uuid():
    data = sys.stdin.read()
    for m in re.finditer(UUID_PATTERN, data):
        print(m.group())
        return
    print('')

def extract_all_uuids():
    data = sys.stdin.read()
    for m in re.finditer(UUID_PATTERN, data):
        print(m.group())

def dump_raw():
    data = sys.stdin.read()
    print(repr(data[:500]))

if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'first_uuid':
        extract_first_uuid()
    elif cmd == 'all_uuids':
        extract_all_uuids()
    elif cmd == 'dump':
        dump_raw()
