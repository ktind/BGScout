package com.ktind.cgm.bgscout.DexcomG4;

/**
 Copyright (c) 2014, Kevin Lee (klee24@gmail.com)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this
 list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public enum G4RcvrCmd {
        NUL("Nul",(byte)0x00),
        ACK("Ack",(byte)0x01),
        NAK("Nak",(byte)0x02),
        INVALIDCOMMAND("InvalidCommand",(byte)0x03),
        INVALIDPARAM("InvalidParam",(byte)0x04),
        INCOMPLETEPACKETRECEIVED("IncompletePacketReceived",(byte)0x05),
        RECEIVERERROR("ReceiverError",(byte)0x06),
        INVALIDMODE("InvalidMode",(byte)0x07),
        PING("Ping",(byte)0x0A,6),
        READFIRMWAREHEADER("ReadFirmwareHeader",(byte)0x0B,6),
        READDATABASEPARTITIONINFO("ReadDatabasePartitionInfo",(byte)0x0F,6),
        READDATABASEPAGERANGE("ReadDatabasePageRange",(byte)0x10,7),
        READDATABASEPAGES("ReadDatabasePages",(byte)0x11,12),
        READDATABASEPAGEHEADER("ReadDatabasePageHeader",(byte)0x12,11),
        READTRANSMITTERID("ReadTransmitterID",(byte)0x19,6),
        WRITETRANSMITTERID("WriteTransmitterID",(byte)0x20),
        READLANGUAGE("ReadLanguage",(byte)0x1B,6),
        WRITELANGUAGE("WriteLanguage",(byte)0x1C,8),
        READDISPLAYTIMEOFFSET("ReadDisplayTimeOffset",(byte)0x1D,6),
        WRITEDISPLAYTIMEOFFSET("WriteDisplayTimeOffset",(byte)0x1E,10),
        READRTC("ReadRTC",(byte)0x1F,6),
        RESETRECEIVER("ResetReceiver",(byte)0x20,6),
        READBATTERYLEVEL("ReadBatteryLevel",(byte)0x21,6),
        READSYSTEMTIME("ReadSystemTime",(byte)0x22,6),
        READSYSTEMTIMEOFFSET("ReadSystemTimeOffset",(byte)0x23),
        WRITESYSTEMTIME("WriteSystemTime",(byte)0x24,6),
        READGLUCOSEUNIT("ReadGlucoseUnit",(byte)0x25,6),
        WRITEGLUCOSEUNIT("WriteGlucoseUnit",(byte)0x26,7),
        READBLINDEDMODE("ReadBlindedMode",(byte)0x27,6),
        WRITEBLINDEDMODE("WriteBlindedMode",(byte)0x28,7),
        READCLOCKMODE("ReadClockMode",(byte)0x29,6),
        WRITECLOCKMODE("WriteClockMode",(byte)0x2A,7),
        READDEVICEMODE("ReadDeviceMode",(byte)0x2B,6),
        ERASEDATABASE("EraseDatabase",(byte)0x2D),
        SHUTDOWNRECEIVER("ShutdownReceiver",(byte)0x2E,6),
        WRITEPCPARAMETERS("WritePCParameters",(byte)0x2F),
        READBATTERYSTATE("ReadBatteryState",(byte)0x30,6),
        READHARDWAREBOARDID("ReadHardwareBoardId",(byte)0x31,6),
        ENTERFIRMWAREUPGRADEMODE("EnterFirmwareUpgradeMode",(byte)0x32),
        READFLASHPAGE("ReadFlashPage",(byte)0x33,10),
        WRITEFLASHPAGE("WriteFlashPage",(byte)0x34),
        ENTERSAMBAACCESSMODE("EnterSambaAccessMode",(byte)0x35),
        READFIRMWARESETTINGS("ReadFirmwareSettings",(byte)0x36),
        READENABLESETUPWIZARDFLAG("ReadEnableSetupWizardFlag",(byte)0x37),
        WRITEENABLESETUPWIZARDFLAG("WriteEnableSetUpWizardFlag",(byte)0x38),
        READSETUPWIZARDSTATE("ReadSetUpWizardState",(byte)0x39),
        WRITESETUPWIZARDSTATE("WriteSetupWizardState",(byte)0x3A),
        MAXCOMMAND("MaxCommand",(byte)0x3B),
        MAXPOSSIBLECOMMAND("MaxPossibleCommand",(byte)0xFF);


        private String stringValue;
        private byte byteVal;
        // This is the expected size of the command to be written.
        // Todo: determine how to variable sized commands. Perhaps if the value is 0?
        private int cmdSize;

        public byte getValue(){
            return byteVal;
        }

        private G4RcvrCmd(String toString,byte value, int size){
            stringValue=toString;
            byteVal=value;
            cmdSize=size;
        }

        private G4RcvrCmd(String toString, byte value){
            stringValue=toString;
            byteVal=value;
            cmdSize=-1;
        }

        public int getCmdSize() {
            return cmdSize;
        }

        @Override
        public String toString(){
            return stringValue;
        }
}
