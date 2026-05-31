import json, sys, re

UUID_PATTERN = r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

def pick_recipe():
    raw = sys.stdin.read()
    # Try JSON first
    try:
        recipes = json.loads(raw)
        if isinstance(recipes, list) and len(recipes) > 0:
            for r in recipes:
                name = r.get('name', '').lower()
                if 'samsung' in name or 'pixel' in name or 'galaxy' in name:
                    print(r['uuid'])
                    return
            print(recipes[0]['uuid'])
            return
    except json.JSONDecodeError:
        pass
    # Fallback: extract first UUID from text
    uuids = re.findall(UUID_PATTERN, raw)
    if uuids:
        print(uuids[0])
    else:
        print('')

def parse_instance():
    raw = sys.stdin.read()
    try:
        data = json.loads(raw)
        print(data.get('uuid', ''))
    except json.JSONDecodeError:
        uuids = re.findall(UUID_PATTERN, raw)
        print(uuids[0] if uuids else '')


if __name__ == '__main__':
    cmd = sys.argv[1] if len(sys.argv) > 1 else ''
    if cmd == 'pick_recipe':
        pick_recipe()
    elif cmd == 'parse_instance':
        parse_instance()
