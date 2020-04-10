import re

p = re.compile('%[^`\s]*')

file1 = open('deviceinfo.md', 'r') 
Lines = file1.readlines() 

ls = []
  
for line in Lines: 
    ls.extend(p.findall(line))

print(set(ls))