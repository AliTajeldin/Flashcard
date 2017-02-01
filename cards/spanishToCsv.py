#!/usr/bin/env python
import csv

class ItemsParser():
    def __init__(self, fileName):
        self.fileName = fileName
        self.fp = file(fileName, "r")

    def readNextItem(self, fp):
        item={}
        for num, line in enumerate(fp):
            line = line.strip()
            if line.startswith("s="):
                item["s"] = line[2:].strip()
            elif line.startswith("e="):
                item["e"] = line[2:].strip()
            elif line.startswith("--"):
                break
            else:
                raise ValueError("Error(" + str(num+1) + "): " + line)
        if item and (not item.has_key("s") or not item.has_key("e")):
            raise ValueError("Missing e/s:" + str(item))
        return item

    def readItems(self):
        items = []
        # skip header line
        self.fp.readline()
        item = self.readNextItem(self.fp)
        while item:
            items.append(item)
            item = self.readNextItem(self.fp)
        return items

p = ItemsParser("spanish.txt")
items = p.readItems()
print(len(items))

with open('spanish.csv', 'wb') as csvfile:
    w = csv.writer(csvfile, delimiter=',',
                            quotechar='"', quoting=csv.QUOTE_MINIMAL)
    for idx, item in enumerate(items):
        w.writerow([item["s"], item["e"]])
