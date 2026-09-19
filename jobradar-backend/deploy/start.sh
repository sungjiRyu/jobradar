#!/bin/sh
set -eu

# 인증서는 MySQL Connector/J에만 적용한다. Valkey/외부 HTTPS는 JVM 공인 CA 사용.
if [ -n "${DB_CA_CERT:-}" ]; then
    umask 077
    ca_file=$(mktemp /tmp/jobradar-mysql-ca.XXXXXX.pem)
    printf '%s\n' "$DB_CA_CERT" > "$ca_file"
    DB_CA_CERT_PATH="$ca_file"
fi

if [ -n "${DB_CA_CERT_PATH:-}" ]; then
    umask 077
    trust_dir=$(mktemp -d /tmp/jobradar-mysql-ca.XXXXXX)
    export DB_TRUSTSTORE_PATH="$trust_dir/truststore.p12"
    keytool -importcert -noprompt -alias aiven-mysql \
        -file "$DB_CA_CERT_PATH" -keystore "$DB_TRUSTSTORE_PATH" \
        -storetype PKCS12 -storepass changeit
fi

exec java -jar "${APP_JAR_PATH:-/app/app.jar}" "$@"
