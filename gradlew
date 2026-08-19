#!/bin/sh
# Gradle wrapper script for Unix

DIRNAME=`dirname "$0"`
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
