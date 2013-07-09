#
# Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

#
# Makefile for Maxine Virtual Edition guest
debug ?= y

ifndef XEN_ROOT 
  $(error "Must set XEN_ROOT environment variable to the root of Xen tree")
endif

ifndef JAVA_HOME 
  $(error "Must set JAVA_HOME environment variable to your JDK home directory")
endif

include $(XEN_ROOT)/Config.mk
OSNAME ?= $(shell uname -s)
ifeq ($(OSNAME),Linux)
  HOSTOS_LC = linux
else
ifeq ($(OSNAME),SunOS)
  HOSTOS_LC = solaris
else
  $(error "Unknown operating system")
endif
endif

# Set TARGET_ARCH
# override TARGET_ARCH     := $(XEN_TARGET_ARCH)
TARGET_ARCH = x86_64

XEN_INTERFACE_VERSION := 0x00030203

PROJECT_ROOT = ../..
GUK_ROOT = $(PROJECT_ROOT)/guk
MAXINE_NATIVE = $(PROJECT_ROOT)/maxine/com.oracle.max.vm.native
MAXINE_SUBSTRATE = $(MAXINE_NATIVE)/generated/maxve
MAXINE_JNI = $(MAXINE_NATIVE)/jni
MAXINE_SHARE = $(MAXINE_NATIVE)/share
MAXINE_INCL = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(HOSTOS_LC) -I$(MAXINE_NATIVE)/share -I$(MAXINE_NATIVE)/platform -I$(MAXINE_NATIVE)/substrate
LIB_MAXINE = libjvm.a

FS_DIR = $(GUK_ROOT)/tools/fs-back
DB_DIR = $(GUK_ROOT)/tools/db-front

# find gcc headers
GCC_BASE=$(shell $(CC) -print-search-dirs | grep ^install | cut -f 2 -d ' ')

ifeq ($(XEN_OS),SunOS)
GCC_INCLUDE:=${GCC_BASE}install-tools/include
endif

ifeq ($(XEN_OS),Linux)
GCC_INCLUDE:=${GCC_BASE}include
GCC_INCLUDE+=-I${GCC_BASE}include-fixed
endif

# NB. '-Wcast-qual' is nasty, so I omitted it.
CFLAGS := -fno-builtin -Wall -Werror -Wredundant-decls -Wno-format -nostdinc -I$(GCC_INCLUDE)
CFLAGS += -Wstrict-prototypes -Wnested-externs -Wpointer-arith -Winline
CFLAGS += -ffixed-r14
CFLAGS += -D__XEN_INTERFACE_VERSION__=$(XEN_INTERFACE_VERSION)

ASFLAGS = -D__ASSEMBLY__

LDLIBS :=
LDS := maxve_$(TARGET_ARCH).lds
LDFLAGS_FINAL := -N -T $(LDS)

# For possible special source directories.
EXTRA_SRC =
# For possible special header directories.
EXTRA_INC =

# Standard name for architecture specific subdirectories.
TARGET_ARCH_DIR = $(TARGET_ARCH)
# This is used for architecture specific links.
ARCH_LINKS =

ifeq ($(TARGET_ARCH),x86_64)
  ifeq ($(OSNAME),Linux)
  CFLAGS += -fno-stack-protector 
  endif
CFLAGS += -m64 -mno-red-zone -fpic -fno-reorder-blocks
CFLAGS += -fno-asynchronous-unwind-tables
# This is for Maxine includes
CFLAGS += -Dw64 
LDFLAGS += -m elf_x86_64
TARGET_ARCH_DIR = x86
EXTRA_INC += $(TARGET_ARCH_DIR)/$(TARGET_ARCH)
EXTRA_SRC += arch/$(EXTRA_INC)
endif

ifeq ($(debug),y)
CFLAGS += -g
else
CFLAGS += -O3
endif

# Add the special header directories to the include paths.
extra_incl := $(foreach dir,$(EXTRA_INC),-I$(GUK_ROOT)/include/$(dir))
override CPPFLAGS := -I. -I$(FS_DIR) -I$(DB_DIR) -I$(GUK_ROOT)/include -I$(GUK_ROOT)/include/guk $(CPPFLAGS) -I$(GUK_ROOT)/include/$(TARGET_ARCH_DIR) $(extra_incl) $(MAXINE_INCL) -Ifdlibm

CFLAGS += -DCONFIG_SMP -DCONFIG_PREEMPT -D$(HOSTOS_LC)
TARGET := maxve
ifdef TARGET_NAME
  TARGET:=$(TARGET)-$(TARGET_NAME)
endif

UKERNEL_OBJ := $(GUK_ROOT)/guk.o
OBJS := $(patsubst %.c,objs/%.o,$(wildcard *.c))
FDLIBMOBJS := $(patsubst fdlibm/%.c,fdlibmobjs/%.o,$(wildcard fdlibm/*.c))
FDLIBMLIBA = libfdlibm.a

LDLIBS += ${MAXINE_SUBSTRATE}/${LIB_MAXINE}

HDSR := $(wildcard *.h)
HDRS += $(wildcard $(GUK_ROOT)/include/*.h)
HDRS += $(wildcard $(GUK_ROOT)/include/xen/*.h)
HDRS += $(wildcard $(GUK_ROOT)/include/$(TARGET_ARCH_DIR)/*.h)
# For special wanted header directories.
extra_heads := $(foreach dir,$(EXTRA_INC),$(wildcard $(dir)/*.h))
HDRS += $(extra_heads)

.PHONY: default
default: $(TARGET)

$(TARGET):  $(OBJS) fdlibmobjs/$(FDLIBMLIBA) $(UKERNEL_OBJ) ${MAXINE_SUBSTRATE}/${LIB_MAXINE} $(LDS)
	$(LD) $(LDFLAGS_FINAL) $(UKERNEL_OBJ) $(OBJS) fdlibmobjs/$(FDLIBMLIBA) $(LDLIBS) -o $@
	if [ ! -e ${MAXINE_SUBSTRATE}/maxvm ]; then ln -s `pwd`/maxve ${MAXINE_SUBSTRATE}/maxvm; fi
#	gzip -f -9 -c $@ >$@.gz

.PHONY: clean
clean:
	find . -type f -name '*.o' | xargs rm -f
	rm -f *.o *~ core $(TARGET).elf $(TARGET).raw $(TARGET) $(TARGET).gz
	find . -type l | xargs rm -f
	rm -f tags TAGS

objs/%.o: %.c $(HDRS) Makefile
	mkdir -p objs
	$(CC) $(CFLAGS) $(CPPFLAGS) -c $< -o $@

%.E: %.c $(HDRS)
	$(CC) $(CFLAGS) $(CPPFLAGS) -E $< -o $@

fdlibmobjs/%.o: fdlibm/%.c  Makefile
	mkdir -p fdlibmobjs
	$(CC) -m64 -c -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(HOSTOS_LC) $< -o $@

fdlibmobjs/$(FDLIBMLIBA): $(FDLIBMOBJS)
	(cd fdlibmobjs; $(AR) r $(FDLIBMLIBA) *.o)

define all_sources
     ( find . -follow -name SCCS -prune -o -name '*.[chS]' -print )
endef

.PHONY: cscope
cscope:
	$(all_sources) > cscope.files
	cscope -k -b -q

.PHONY: tags
tags:
	$(all_sources) | xargs ctags
