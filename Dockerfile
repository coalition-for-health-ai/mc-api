ARG JAVA_VERSION=17
# This image additionally contains function core tools – useful when using custom extensions
#FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION-core-tools AS installer-env
FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION-build AS installer-env

ENV PATH="/usr/lib/jvm/java-$JAVA_VERSION-openjdk/bin:${PATH}"

COPY . /src/java-function-app
RUN cd /src/java-function-app && \
    mkdir -p /home/site/wwwroot && \
    mvn clean package && \
    cd ./target/azure-functions/ && \
    cd $(ls -d */|head -n 1) && \
    cp -a . /home/site/wwwroot

# This image is ssh enabled
FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION-appservice
# This image isn't ssh enabled
#FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION

RUN mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium --with-deps --only-shell"

ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

COPY --from=installer-env ["/home/site/wwwroot", "/home/site/wwwroot"]