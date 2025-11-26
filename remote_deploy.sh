#!/bin/bash
set -e

REMOTE_PATH="/root/project/work"
PROJECT_NAME="docker-vue-java-mysql"
ARCHIVE_NAME="docker-vue-java-mysql.tar.gz"

cd "$REMOTE_PATH"

if [ -d "$PROJECT_NAME" ]; then
    if [ -d "${PROJECT_NAME}_backup" ]; then
        rm -rf "${PROJECT_NAME}_backup"
    fi
    mv "$PROJECT_NAME" "${PROJECT_NAME}_backup"
fi

echo "Extracting archive..."
tar -xzf "$ARCHIVE_NAME" 2>&1 | grep -v "Ignoring unknown extended header" || true

REMOTE_DIR=$(tar -tzf "$ARCHIVE_NAME" | head -1 | cut -d'/' -f1)
if [ -d "$REMOTE_DIR" ] && [ "$REMOTE_DIR" != "$PROJECT_NAME" ]; then
    echo "Renaming directory from $REMOTE_DIR to $PROJECT_NAME"
    mv "$REMOTE_DIR" "$PROJECT_NAME"
fi

if [ -d "$PROJECT_NAME" ]; then
    cd "$PROJECT_NAME"
    # Fix Windows line ending issue
    sed -i 's/\r$//' deploy.sh
    chmod +x deploy.sh
    ./deploy.sh
else
    echo "Error: Cannot find $PROJECT_NAME directory after extraction"
    exit 1
fi
