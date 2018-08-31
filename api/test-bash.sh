if [ $(git diff --name-only $(git merge-base master ch/ci-strawman) | grep api/ | wc -l | xargs) == 0 ] && [ ch/ci-strawman != "master" ]; then
    echo $(git diff --name-only $(git merge-base master ch/ci-strawman) | grep api/ | wc -l)
    echo No relevant changes on non-master branch, skipping
fi