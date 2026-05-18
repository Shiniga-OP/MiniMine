MSG=$1
git add .
git commit -m "$MSG"
git gc --prune=now --aggressive
git push
