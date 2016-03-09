FROM java:openjdk-8-jre

COPY service/target/libs/commercetools-payone-integration.jar /build/commercetools-payone-integration.jar

CMD ["java", "-jar", "/build/commercetools-payone-integration.jar"]

EXPOSE 8080
