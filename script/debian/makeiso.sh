#!/bin/bash
set -o errexit
set -o nounset

if [[ -z ${1:-} ]]; then
    echo Usage: $(basename $0) USER
    exit 2
fi

if [[ $EUID -ne 0 ]]; then
    echo "This script must be run as root" 1>&2
    exit 1
fi

mkdir -p binary/live binary/isolinux

sudo apt-get install live-build syslinux squashfs-tools xorriso
sudo debootstrap --arch=i386 wheezy chroot
sudo chroot chroot /bin/bash -x <<EOF
export HOME=/root
export LC_ALL=C

mount none -t proc /proc
mount none -t sysfs /sys
mount none -t devpts /dev/pts

apt-get -y install dialog dbus
dbus-uuidgen > chroot/var/lib/dbus/machine-id
apt-get -y install linux-image-486 live-boot
apt-get -y install ca-certificates gcc less python python-dev python-pip

pip install pycrypto requests

apt-get -y purge binutils bzip2 cpp dpkg-dev gcc make python-dev python-pip
apt-get -y purge aptitude fakeroot perl tasksel traceroute vim-tiny
apt-get -y purge debconf-i18n file info man manpages manpages-dev man-db
apt-get -y autoremove
apt-get -y clean

cd /usr/local/bin/
wget -O pssst https://raw.github.com/pssst/pssst/master/app/cli/pssst.py
chmod a+x /usr/local/bin/pssst

echo ALL: ALL EXCEPT api.pssst.name > /etc/hosts.deny
echo pssst > /etc/hostname
echo pssst --help >> /root/.bashrc
echo "root:pssst" | chpasswd

rm -rf /tmp/* /usr/share/doc/* /usr/share/info/* /usr/share/man/*
rm -f /etc/inittab /var/lib/dbus/machine-id
umount /proc /sys /dev/pts
exit
EOF

cat > binary/isolinux/isolinux.cfg <<EOF
prompt 0
timeout 0
default auto
ontimeout auto

label auto
  linux /live/vmlinuz
    append initrd=/live/initrd boot=live persistence quiet
EOF

cat <<EOF | sudo tee -a chroot/etc/inittab
id:2:initdefault:
si::sysinit:/etc/init.d/rcS
~~:S:wait:/sbin/sulogin
l0:0:wait:/etc/init.d/rc 0
l1:1:wait:/etc/init.d/rc 1
l2:2:wait:/etc/init.d/rc 2
l3:3:wait:/etc/init.d/rc 3
l4:4:wait:/etc/init.d/rc 4
l5:5:wait:/etc/init.d/rc 5
l6:6:wait:/etc/init.d/rc 6
z6:6:respawn:/sbin/sulogin
ca:12345:ctrlaltdel:/sbin/shutdown -t1 -a -r now
pf::powerwait:/etc/init.d/powerfail start
pn::powerfailnow:/etc/init.d/powerfail now
po::powerokwait:/etc/init.d/powerfail stop
1:2345:respawn:/sbin/getty --autologin root 38400 tty1
EOF

sudo cp /home/$1/.pssst.* chroot/root/ || true
sudo cp chroot/boot/vmlinuz-* binary/live/vmlinuz
sudo cp chroot/boot/initrd.img-* binary/live/initrd
sudo cp /usr/lib/syslinux/isolinux.bin binary/isolinux/

sudo mksquashfs chroot binary/live/filesystem.squashfs -comp xz -e boot

xorriso -as mkisofs -r -J -joliet-long -l \
  -isohybrid-mbr /usr/lib/syslinux/isohdpfx.bin -partition_offset 16 \
  -A "Pssst" -b isolinux/isolinux.bin -c isolinux/boot.cat \
  -no-emul-boot -boot-load-size 4 -boot-info-table -o pssst.iso binary

sudo rm -rf binary/ chroot/

echo "Created file pssst.iso"
exit 0