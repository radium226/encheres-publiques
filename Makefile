/tmp/makepkg:
	mkdir -p "/tmp/makepkg"

.PHONY:
makepkg: /tmp/makepkg
	cd "./archlinux-package" && \
	makepkg \
		--syncdeps \
		--skipchecksums \
		--noconfirm \
		--clean \
		--cleanbuild
