# SEE https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javapackager.html

JAVA_HOME=`/usr/libexec/java_home -v 9`
APP_DIR_NAME=DiscOmicsPS.app

#-deploy -Bruntime=/Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home \
javapackager \
  -deploy -Bruntime=${JAVA_HOME} \
  -native image \
  -BjvmOptions=-Xmx12g \
  -BjvmOptions=-XX:+UseG1GC \
  -BjvmOptions=-XX:+UseStringDeduplication \
  -srcdir . \
  -srcfiles DiscOmicsPS.jar \
  -outdir release \
  -outfile ${APP_DIR_NAME} \
  -appclass discomics.Main \
  -name "DiscOmicsPS" \
  -title "DiscOmicsPS" \
  -nosign \
  -v

echo ""
echo "If that succeeded, it created \"release/bundles/${APP_DIR_NAME}\""