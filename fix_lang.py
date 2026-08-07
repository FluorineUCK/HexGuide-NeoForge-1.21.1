def fix_lang(path):
    s = open(path, encoding='utf-8').read()
    # 1) 删除重复的 entry/page 块（categories 后、demo 前）
    pattern = '    entry: {\n      notes: {\n        items: "TODO: fill",\n      },\n    },\n\n    page: {\n      "notes": {\n        items: {\n          "0": "TODO: fill",\n          "1": "TODO: fill",\n          "2": "TODO: fill",\n          "3": "TODO: fill",\n          "4": "TODO: fill",\n          "5": "TODO: fill",\n        },\n      },\n    },\n\n'
    assert pattern in s, f"{path}: entry/page block not found"
    s = s.replace(pattern, '')
    # 2) 在原 entry 块加 notes: { items }
    old_entry = '    entry: {\n      search: "Book Search",'
    new_entry = '    entry: {\n      notes: {\n        items: "TODO: fill",\n      },\n      search: "Book Search",'
    assert old_entry in s, f"{path}: entry anchor not found"
    s = s.replace(old_entry, new_entry)
    # 3) 在原 page 块加 notes.items
    old_page = '    page: {\n      search: {'
    new_page = '    page: {\n      "notes": {\n        items: {\n          "0": "TODO: fill",\n          "1": "TODO: fill",\n          "2": "TODO: fill",\n          "3": "TODO: fill",\n          "4": "TODO: fill",\n          "5": "TODO: fill",\n        },\n      },\n      search: {'
    assert old_page in s, f"{path}: page anchor not found"
    s = s.replace(old_page, new_page)
    open(path, 'w', encoding='utf-8').write(s)
    print("fixed", path)

fix_lang('common/src/main/resources/assets/hexguide/lang/en_us.flatten.json5')
fix_lang('common/src/main/resources/assets/hexguide/lang/zh_cn.flatten.json5')
