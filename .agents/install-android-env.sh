#!/usr/bin/env bash
set -euo pipefail

CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
ANDROID_SDK_DIR="$HOME/Android/Sdk"
ENV_HELPER="$HOME/.local/bin/android-env.sh"

# Ensure non-interactive bash shells execute ~/.bashrc to the end.
if [ -f "$HOME/.bashrc" ]; then
  sed -i '/case \$- in/,/esac/d' "$HOME/.bashrc" 2>/dev/null || true
else
  touch "$HOME/.bashrc"
fi

mkdir -p "$HOME/.local/bin"

cat <<'EOF' > "$ENV_HELPER"
#!/usr/bin/env bash
# Configures Java, Android SDK, and proxy/CA settings for non-interactive shells.

if [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
  export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
elif [ -z "${JAVA_HOME:-}" ] && command -v javac >/dev/null 2>&1; then
  export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
fi

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

case ":$PATH:" in
  *":$ANDROID_HOME/cmdline-tools/latest/bin:"*) ;;
  *) export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH" ;;
esac

case ":$PATH:" in
  *":$ANDROID_HOME/platform-tools:"*) ;;
  *) export PATH="$ANDROID_HOME/platform-tools:$PATH" ;;
esac

_android_env_java_opts=""
_android_env_gradle_props=""

# Build user-space Java truststore when system Java cacerts is missing.
if [ ! -e "/etc/ssl/certs/java/cacerts" ] && [ -r "/etc/ssl/certs/ca-certificates.crt" ]; then
  _cacerts_dir="$HOME/.local/share/java"
  _cacerts_file="$_cacerts_dir/cacerts"
  _cacerts_hash="$_cacerts_dir/cacerts.sha256"
  _cur_hash="$(sha256sum /etc/ssl/certs/ca-certificates.crt | awk '{print $1}')"
  _old_hash=""
  if [ -r "$_cacerts_hash" ]; then
    _old_hash="$(cat "$_cacerts_hash")"
  fi
  if [ ! -f "$_cacerts_file" ] || [ "$_cur_hash" != "$_old_hash" ]; then
    mkdir -p "$_cacerts_dir"
    _tmp_store="$(mktemp "$_cacerts_dir/cacerts.XXXXXX")"
    rm -f "$_tmp_store"
    _tmp_dir="$(mktemp -d)"
    awk -v dir="$_tmp_dir" '
      /-----BEGIN CERTIFICATE-----/ { n++; f = sprintf("%s/cert_%04d.pem", dir, n) }
      f != "" { print > f }
      /-----END CERTIFICATE-----/ { close(f); f = "" }
    ' /etc/ssl/certs/ca-certificates.crt
    for _pem in "$_tmp_dir"/cert_*.pem; do
      [ -f "$_pem" ] || continue
      keytool -importcert -noprompt -trustcacerts \
        -alias "$(basename "$_pem" .pem)" \
        -file "$_pem" \
        -keystore "$_tmp_store" \
        -storepass changeit \
        -storetype PKCS12 >/dev/null 2>&1 || true
    done
    rm -rf "$_tmp_dir"
    if [ -f "$_tmp_store" ]; then
      mv -f "$_tmp_store" "$_cacerts_file"
      printf '%s\n' "$_cur_hash" > "$_cacerts_hash"
    fi
  fi
  if [ -f "$_cacerts_file" ]; then
    _android_env_java_opts="$_android_env_java_opts -Djavax.net.ssl.trustStore=$_cacerts_file -Djavax.net.ssl.trustStorePassword=changeit"
    _android_env_gradle_props="${_android_env_gradle_props}systemProp.javax.net.ssl.trustStore=$_cacerts_file
systemProp.javax.net.ssl.trustStorePassword=changeit
"
  fi
fi

# Configure Java/Gradle HTTP(S) proxy from environment when present.
_proxy_url="${HTTPS_PROXY:-${HTTP_PROXY:-${https_proxy:-${http_proxy:-}}}}"
if [ -n "$_proxy_url" ]; then
  _proxy_hp="${_proxy_url#http://}"
  _proxy_hp="${_proxy_hp#https://}"
  _proxy_hp="${_proxy_hp%/}"
  _proxy_host="${_proxy_hp%:*}"
  _proxy_port="${_proxy_hp##*:}"
  if [ -n "$_proxy_host" ] && [ -n "$_proxy_port" ] && [ "$_proxy_host" != "$_proxy_port" ]; then
    _android_env_java_opts="$_android_env_java_opts -Dhttp.proxyHost=$_proxy_host -Dhttp.proxyPort=$_proxy_port -Dhttps.proxyHost=$_proxy_host -Dhttps.proxyPort=$_proxy_port -Dhttp.nonProxyHosts=localhost"
    _android_env_gradle_props="${_android_env_gradle_props}systemProp.http.proxyHost=$_proxy_host
systemProp.http.proxyPort=$_proxy_port
systemProp.https.proxyHost=$_proxy_host
systemProp.https.proxyPort=$_proxy_port
systemProp.http.nonProxyHosts=localhost|127.0.0.1
"
  fi
fi

if [ -n "$_android_env_java_opts" ]; then
  export JAVA_OPTS="$_android_env_java_opts"
  export GRADLE_OPTS="$_android_env_java_opts"
fi

if [ -n "$_android_env_gradle_props" ]; then
  mkdir -p "$HOME/.gradle"
  _gradle_props_file="$HOME/.gradle/gradle.properties"
  _desired_props="# Auto-managed by android-env.sh
${_android_env_gradle_props}"
  if [ ! -f "$_gradle_props_file" ] || [ "$(cat "$_gradle_props_file" 2>/dev/null)" != "$_desired_props" ]; then
    printf '%s' "$_desired_props" > "$_gradle_props_file"
  fi
fi

unset _android_env_java_opts _android_env_gradle_props _cacerts_dir _cacerts_file _cacerts_hash _cur_hash _old_hash _tmp_store _tmp_dir _pem _proxy_url _proxy_hp _proxy_host _proxy_port _gradle_props_file _desired_props
EOF

chmod +x "$ENV_HELPER"

BASHRC_LINE='[ -f "$HOME/.local/bin/android-env.sh" ] && source "$HOME/.local/bin/android-env.sh"'
grep -qxF "$BASHRC_LINE" "$HOME/.bashrc" || printf '%s\n' "$BASHRC_LINE" >> "$HOME/.bashrc"

# shellcheck disable=SC1090
source "$ENV_HELPER"

# Install Android SDK command-line tools if missing.
SDKMANAGER="$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
  TMP_DIR="$(mktemp -d)"
  trap 'rm -rf "$TMP_DIR"' EXIT
  curl -fsSL "$CMDLINE_TOOLS_URL" -o "$TMP_DIR/cmdline-tools.zip"
  unzip -q "$TMP_DIR/cmdline-tools.zip" -d "$TMP_DIR/extract"
  rm -rf "$ANDROID_SDK_DIR/cmdline-tools/latest"
  mv "$TMP_DIR/extract/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest"
  rm -rf "$TMP_DIR"
  trap - EXIT
fi

# Accept SDK licenses non-interactively.
(
  set +o pipefail
  yes | "$SDKMANAGER" --sdk_root="$ANDROID_SDK_DIR" --licenses >/dev/null 2>&1 || true
)

# Install required Android SDK packages if any are missing.
MISSING_PACKAGES=()
[ -d "$ANDROID_SDK_DIR/platform-tools" ] || MISSING_PACKAGES+=("platform-tools")
[ -d "$ANDROID_SDK_DIR/platforms/android-34" ] || MISSING_PACKAGES+=("platforms;android-34")
[ -d "$ANDROID_SDK_DIR/build-tools/33.0.1" ] || MISSING_PACKAGES+=("build-tools;33.0.1")
[ -d "$ANDROID_SDK_DIR/build-tools/34.0.0" ] || MISSING_PACKAGES+=("build-tools;34.0.0")

if [ "${#MISSING_PACKAGES[@]}" -gt 0 ]; then
  "$SDKMANAGER" --sdk_root="$ANDROID_SDK_DIR" "${MISSING_PACKAGES[@]}"
fi

# Warm Gradle wrapper and dependency cache, and verify unit tests pass.
(
  cd "E-Stock Noted"
  sh ./gradlew --no-daemon testDebugUnitTest --console=plain
)
