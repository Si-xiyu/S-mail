#!/bin/bash
MAVEN="$HOME/apache-maven-3.9.9/bin/mvn"
BUILD_DIR="/tmp/smartmail-build"

# 复制到无中文路径编译
echo ">>> 编译中..."
rm -rf "$BUILD_DIR"
cp -r "$(dirname "$0")" "$BUILD_DIR"
"$MAVEN" -f "$BUILD_DIR/pom.xml" compile -DskipTests -q
cp -r "$BUILD_DIR/target" "$(dirname "$0")/target"

# 用 Maven 直接启动 Spring Boot
echo ">>> 启动 SmartMail (dev profile)..."
"$MAVEN" -f "$BUILD_DIR/pom.xml" spring-boot:run -Dspring-boot.run.profiles=dev
