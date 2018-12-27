pkgname="encheres-publiques-git"
pkgver=r18.6c09348
pkgrel="1"

arch=('any')

makedepends=(
  "scala"
  "sbt"
)

pkgver() {
  cd "${pkgname%-git}"
  printf "r%s.%s" "$(git rev-list --count HEAD)" "$(git rev-parse --short HEAD)"
}

depends=(
  "firefox"
  "geckodriver"
  "xorg-server-xvfb"
  "java-runtime"
  "xdotool"
  "ffmpeg"
)

source=(
  "git+https://github.com/radium226/encheres-publiques.git"
  "usr_lib_systemd_system_encheres-publiques.service"
  "usr_lib_systemd_system_encheres-publiques.timer"
  "usr_lib_sysusers.d_encheres-publiques.conf"
  "usr_lib_tmpfiles.d_encheres-publiques.conf"
  "usr_bin_encheres-publiques"
)

build() {
  cd "${pkgname%-git}"
  sbt assembly
  cd -
}

package() {
  install -Dm0644 \
    "${pkgname%-git}/target/scala-2.12/encheres-publiques-assembly-0.1-SNAPSHOT.jar" \
    "${pkgdir}/usr/share/java/encheres-publiques/encheres-publiques.jar"

  install -Dm0755 \
    "usr_bin_encheres-publiques" \
    "${pkgdir}/usr/bin/encheres-publiques"

  install -Dm0644 \
    "usr_lib_systemd_system_encheres-publiques.service" \
    "${pkgdir}/usr/lib/systemd/system/encheres-publiques.service"

  install -Dm0644 \
    "usr_lib_systemd_system_encheres-publiques.timer" \
    "${pkgdir}/usr/lib/systemd/system/encheres-publiques.timer"

  install -Dm0644 \
    "usr_lib_sysusers.d_encheres-publiques.conf" \
    "${pkgdir}/usr/lib/sysusers.d/encheres-publiques.conf"

  install -Dm0644 \
    "usr_lib_tmpfiles.d_encheres-publiques.conf" \
    "${pkgdir}/usr/lib/tmpfiles.d/encheres-publiques.conf"
}
