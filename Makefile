TOP:=Top
BUILD:=./build
TOP_VLOG:=$(BUILD)/$(TOP).v

MILL:=mill

IIT3503CHISEL:=$(shell find src/main/scala/iit3503/*.scala)

$(TOP_VLOG): $(IIT3503CHISEL)
	@mkdir -p $(@D)
	@$(MILL) iit3503_lab.run iit3503.Top.SimMain -td $(@D) --output-file $(@F)

SIM_TOP = $(TOP)

verilog: $(TOP_VLOG)


SIM_CSRC_DIR:=$(abspath ./src/cpp)
SIM_VSRC_DIR:=$(abspath ./src/v)
SIM_MKFILE:=$(BUILD)/sim-compile/V$(SIM_TOP).mk
SIM_CXXFILES:=$(shell find $(SIM_CSRC_DIR) -name "*.cpp")
SIM_CHDR:= $(shell find $(SIM_CSRC_DIR) -name "*.h")
SIM_CSRC:= $(SIM_CXXFILES) $(SIM_CHDR)
SIM_VFILES:=$(shell find $(SIM_VSRC_DIR) -name "*.v")
SIM_DEPS:= $(SIM_VFILES) $(SIM_CSRC)
SIM_CXXFLAGS = -O3
SIM_LDFLAGS = -lpthread -lreadline
SIM := $(BUILD)/sim

ASM_BIN_DIR:=binaries
ASM_SRC_DIR:=$(abspath ./asm)
ASM_SRC_FILES:=$(shell find $(ASM_SRC_DIR) -name "*.asm")
ASM_OBJ_FILES:=$(addprefix $(ASM_BIN_DIR)/, $(notdir $(ASM_SRC_FILES:%.asm=%.bin)))

VERILATOR_FLAGS = --top-module $(SIM_TOP) \
	-I$(abspath $(BUILD)) \
	-I$(abspath $(SIM_CSRC_DIR)) \
	-CFLAGS "$(SIM_CXXFLAGS)" \
	-LDFLAGS "$(SIM_LDFLAGS)" \
	-Wno-WIDTH\
	--trace

$(SIM_MKFILE): $(TOP_VLOG) 
	@echo "Building simulator config from Chisel output..."
	@mkdir -p $(@D)
	@verilator --cc --exe $(VERILATOR_FLAGS) \
		-o $(abspath $(SIM)) -Mdir $(@D) $^ $(SIM_CXXFILES) $(SIM_VFILES)

$(SIM): $(SIM_MKFILE) $(SIM_DEPS)
	@echo "Building simulator..."
	@$(MAKE) -C $(dir $(SIM_MKFILE)) -f $(abspath $(SIM_MKFILE))

ASSEMBLER:=lc3as

$(ASM_OBJ_FILES): $(ASM_SRC_FILES)

$(ASM_BIN_DIR)/%.bin: $(ASM_SRC_DIR)/%.asm
	@echo "Assembling binary: $(basename $(notdir $<))"
	@mkdir -p $(@D)
	@$(ASSEMBLER) $< 1>/dev/null
	@mv $(basename $<).obj $@
	@rm $(ASM_SRC_DIR)/*.sym


mem_syn.hex: 
	icebram -g 16 1024 > $@

Top.blif Top.json: Top.v
	yosys -p "read_verilog Top.v; synth_ice40 -relut -abc2 -blif Top.blif -json Top.json"

Top_syn.asc: Top.json
	nextpnr-ice40 --hx1k --package vq100 --json $< --pcf pins.pcf --asc $@ --freq 25

# for arachne this would be something like:
# arachne-pnr  -d 1k -p pins.pcf -P vq100 -o Top_syn.asc Top.blif

# TODO: BRAM not initializing correctly right now
#Top.asc: Top_syn.asc mem.hex mem_syn.hex
	#icebram mem_syn.hex mem.hex < $< > $@

# this won't work in WSL2 or in a VM!
program: Top.bin
	iceprog Top.bin

Top.bin: Top_syn.asc
	icepack -s $< $@

mem.hex: binaries/hello.bin
	xxd -p -c 2 -skip +2 $< > $@



#
# This builds the (cycle-accurate) simulator for the 3503. Used to test
# macro functionality (LC-3 programs)
# 
sim: $(SIM) $(ASM_OBJ_FILES) 


#
# These are all unit tests. They test the functionality of individual modules of the 3503
# 
test-useq: src/main/scala/iit3503/MicroSequencer.scala src/test/scala/iit3503/MicroSeqTester.scala
	@sbt 'testOnly iit3503.MicroSeqTester -- -DwriteVcd=1'

test-microcode: src/main/scala/iit3503/ControlStore.scala src/test/scala/iit3503/ControlStoreTester.scala
	@sbt 'testOnly iit3503.ControlStoreTester -- -DwriteVcd=1'

test-control: src/main/scala/iit3503/Control.scala src/test/scala/iit3503/ControlTester.scala
	@sbt 'testOnly iit3503.ControlTester -- -DwriteVcd=1'

test-datapath: src/main/scala/iit3503/DataPath.scala src/test/scala/iit3503/DataPathTester.scala
	@sbt 'testOnly iit3503.DataPathTester -- -DwriteVcd=1'

test-intctrl: src/main/scala/iit3503/IntCtrl.scala src/test/scala/iit3503/IntCtrlTester.scala
	@sbt 'testOnly iit3503.IntCtrlTester -- -DwriteVcd=1'

test-memctrl: src/main/scala/iit3503/MemCtrl.scala src/test/scala/iit3503/MemCtrlTester.scala
	@sbt 'testOnly iit3503.MemCtrlTester -- -DwriteVcd=1'

test-alu: src/main/scala/iit3503/ALU.scala src/test/scala/iit3503/ALUTester.scala
	@sbt 'testOnly iit3503.ALUTester -- -DwriteVcd=1'

test-regs: src/main/scala/iit3503/RegFile.scala src/test/scala/iit3503/RegFileTester.scala
	@sbt 'testOnly iit3503.RegFileTester -- -DwriteVcd=1'

#
# Runs all unit tests at once (this will take a while)
# 
test-all-units: $(IIT3503CHISEL)
	@sbt 'test'


clean-asm:
	@rm -f binaries/* test_asm/*.sym test_asm/*.obj

clean: clean-asm
	@rm -rf ./build out

dist-clean: clean clean-asm
	@rm -rf tools/gen/target

#### HANDIN #####

PROJECT_NAME=iit3503
PROJECT_SERVER="http://subutai.cs.iit.edu"
PROJECT_PORT=5902

handin:
	@echo "Creating tarball and attempting submission..."
	@$(MAKE) clean
	@tar cvzf submission.tar.gz asm/ Makefile README.md src/ build.sbt build.sc >/dev/null 2>&1
	@echo "  submission file successfully created in submission.tar.gz"
	@echo "Initiating submission..."
	@python3 tools/submit.py $(PROJECT_NAME) submission.tar.gz -s $(PROJECT_SERVER) -p $(PROJECT_PORT)
	@echo "Submission complete. Cleaning up."
	@rm -f submission.tar.gz
