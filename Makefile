.PHONY: package
package: ./src/main/makepkg/encheres-publiques.jar
	cd "./src/main/makepkg" && makepkg --force --skipchecksums

.PHONY: clean
clean:
	sbt clean

	find "./src/main/makepkg" -name "*.tar.xz" | \
		xargs -I {} rm "{}"

	test -f "./src/main/makepkg/encheres-publiques.jar" && \
		rm -Rf "./src/main/makepkg/encheres-publiques.jar" || true

	test -d "./src/main/makepkg/src" && \
		rm -Rf "./src/main/makepkg/src" || true

	test -d "./src/main/makepkg/pkg" && \
		rm -Rf "./src/main/makepkg/pkg" || true

./src/main/makepkg/encheres-publiques.jar:
	sbt assembly
	find "./target" -name "*.jar" | head -n1 | xargs -I {} cp "{}" "./src/main/makepkg/encheres-publiques.jar"

.PHONY: install
install: clean package
	cd "./src/main/makepkg" && \
	find "." -name "*.tar.xz" | xargs -I {} yay -U "{}" --noconfirm

.PHONY: install-odroid-xu4
install-odroid-xu4:
	declare package_file_path="$$( readlink -f "$$( find "src/main/makepkg" -name "*.tar.xz" )" )" && \
	cd "../../odroid-xu4/ansible" && \
	make copy-to HOST=rouages.xyz SOURCE="$${package_file_path}" DEST="/tmp/encheres-publiques.tar.xz" && \
	sleep 60 && \
	make ssh HOST=rouages.xyz COMMAND="sudo -u ansible yay -U '/tmp/encheres-publiques.tar.xz' --noconfirm" && \
	sleep 60

.PHONY: run-odroid-xu4
run-odroid-xu4:
	make ssh HOST=rouages.xyz COMMAND="systemctl start encheres-publiques.service" && \
	sleep 60 && \
	make copy-from HOST=rouages.xyz SOURCE="/var/cache/encheres-publiques/browsing.mp4" DEST="./browsing.mp4"