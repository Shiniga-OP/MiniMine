COMEN="$1"

git add .
git commit -m "$COMEN"
git pull --rebase
git push -u origin main