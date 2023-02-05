#!/bin/sh

# purpose: this script creates a signed ".app" application directory for the
#          ACME application
#
# known assumptions for this script:
#   - the application jar files are in the 'lib' directory
#   - the icon file is in the current directory
#   - the necessary resource files are in the 'resources' directory (.ini, etc.)
#   - the necessary apple certificates are installed on the Mac this script is run on
#
# see this URL for details about the `javapackager` command:
# https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javapackager.html

# necessary variables
JAVA_HOME=`/usr/libexec/java_home -v 1.8.0`
RELEASE_DIR=release
APP_DIR_NAME=SunTray.app

# javapackager command notes:
#   - `-native image` creates a ".app" file (as opposed to DMG or other)
#   - `-name` is used as the app name in the menubar if you don't specify "-Bmac.CFBundleName"
#   - oracle notes says "use cms for desktop apps"
#   - `v` is for verbose mode. remove it if you don't want/need to see all of the output

# (1) create and sign the ".app" directory structure. this command creates the
#     "./release/bundles/ACME.app" directory.
javapackager \
  -deploy -Bruntime=${JAVA_HOME} \
  -native image \
  -outdir ${RELEASE_DIR} \
  -outfile ${APP_DIR_NAME} \
  -srcdir target \
  -srcfiles SunTray-1-jar-with-dependencies.jar \
  -appclass com.ericulicny.suntray.SunTray \
  -name "SunTray" \
  -title "SunTray" \
  -vendor "Eric Ulicny Software" \
  -Bicon=src/main/resources/full-moon-icon-md.icns \
  -Bmac.CFBundleVersion=1.0 \
  -Bmac.CFBundleIdentifier=com.ericulicny.suntray.SunTray \
  -BjvmOptions=-Xms128m \
  -BjvmOptions=-XX:+UseConcMarkSweepGC \
  -BjvmOptions=-XX:ParallelCMSThreads=2 \
  -BjvmOptions=-XX:PermSize=20m \
  -BjvmOptions=-XX:MaxPermSize=20m \
  -BjvmOptions=-Dapple.laf.useScreenMenuBar=true \
  -BjvmOptions=-Dcom.apple.smallTabs=true \
  -v

# (2b) copy *all* resource files into the ".app" directory
#cp -R resources/ ${RELEASE_DIR}/bundles/${APP_DIR_NAME}/Contents/Java/
