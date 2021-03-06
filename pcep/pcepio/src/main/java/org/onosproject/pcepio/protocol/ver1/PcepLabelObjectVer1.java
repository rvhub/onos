/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.pcepio.protocol.ver1;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepLabelObject;
import org.onosproject.pcepio.types.NexthopIPv4addressTlv;
import org.onosproject.pcepio.types.NexthopIPv6addressTlv;
import org.onosproject.pcepio.types.NexthopUnnumberedIPv4IDTlv;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP label object.
 */
public class PcepLabelObjectVer1 implements PcepLabelObject {

    /*
     *   ref : draft-zhao-pce-pcep-extension-for-pce-controller-01 , section : 7.4.

        0                   1                   2                   3
           0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | Reserved                      | Flags                       |O|
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                            Label                              |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                                                               |
       //                        Optional TLV                          //
       |                                                               |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                     The LABEL Object format
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepLspObjectVer1.class);

    public static final byte LABEL_OBJ_TYPE = 1;
    public static final byte LABEL_OBJ_CLASS = 35; //TBD : to be defined
    public static final byte LABEL_OBJECT_VERSION = 1;
    public static final byte OBJECT_HEADER_LENGTH = 4;
    public static final boolean DEFAULT_OFLAG = false;

    // LSP_OBJ_MINIMUM_LENGTH = CommonHeaderLen(4)+ LspObjectHeaderLen(8)
    public static final short LABEL_OBJ_MINIMUM_LENGTH = 12;

    public static final int OFLAG_SET = 1;
    public static final int OFLAG_RESET = 0;
    public static final int MINIMUM_COMMON_HEADER_LENGTH = 4;

    static final PcepObjectHeader DEFAULT_LABEL_OBJECT_HEADER = new PcepObjectHeader(LABEL_OBJ_CLASS, LABEL_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, LABEL_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader labelObjHeader;
    private boolean bOFlag;
    private int label;
    // Optional TLV
    private LinkedList<PcepValueType> llOptionalTlv;

    /**
     * Constructor to initialize parameters for PCEP label object.
     *
     * @param labelObjHeader label object header
     * @param bOFlag O flag
     * @param label label
     * @param llOptionalTlv list of optional tlvs
     */
    public PcepLabelObjectVer1(PcepObjectHeader labelObjHeader, boolean bOFlag, int label,
            LinkedList<PcepValueType> llOptionalTlv) {
        this.labelObjHeader = labelObjHeader;
        this.bOFlag = bOFlag;
        this.label = label;
        this.llOptionalTlv = llOptionalTlv;
    }

    @Override
    public LinkedList<PcepValueType> getOptionalTlv() {
        return this.llOptionalTlv;
    }

    @Override
    public void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
        this.llOptionalTlv = llOptionalTlv;
    }

    @Override
    public boolean getOFlag() {
        return this.bOFlag;
    }

    @Override
    public void setOFlag(boolean value) {
        this.bOFlag = value;
    }

    @Override
    public int getLabel() {
        return this.label;
    }

    @Override
    public void setLabel(int value) {
        this.label = value;
    }

    /**
     * Reads form channel buffer and returns objects of PcepLabelObject.
     *
     * @param cb of type channel buffer
     * @return objects of PcepLabelObject
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepLabelObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader labelObjHeader;

        boolean bOFlag;
        int label;

        // Optional TLV
        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();
        labelObjHeader = PcepObjectHeader.read(cb);

        //take only LspObject buffer.
        ChannelBuffer tempCb = cb.readBytes(labelObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);

        int iTemp = tempCb.readInt();
        bOFlag = (iTemp & (byte) 0x01) == 1 ? true : false;
        label = tempCb.readInt();

        // parse optional TLV
        llOptionalTlv = parseOptionalTlv(tempCb);
        return new PcepLabelObjectVer1(labelObjHeader, bOFlag, label, llOptionalTlv);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();
        int objLenIndex = labelObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException(" ObjectLength Index is " + objLenIndex);
        }

        byte oFlag;

        oFlag = (byte) ((bOFlag) ? OFLAG_SET : OFLAG_RESET);
        cb.writeInt(oFlag);
        cb.writeInt(label);

        // Add optional TLV
        packOptionalTlv(cb);

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;

        //will be helpful during print().
        labelObjHeader.setObjLen((short) length);
        cb.setShort(objLenIndex, (short) length);
        return cb.writerIndex();
    }

    /**
     * Returns list of optional tlvs.
     *
     * @param cb of type channel buffer
     * @return list of optional tlvs.
     * @throws PcepParseException when fails to parse list of optional tlvs
     */
    protected static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llOutOptionalTlv = new LinkedList<PcepValueType>();

        while (MINIMUM_COMMON_HEADER_LENGTH <= cb.readableBytes()) {

            PcepValueType tlv;
            short hType = cb.readShort();
            short hLength = cb.readShort();
            int iValue = 0;

            switch (hType) {

            case NexthopIPv4addressTlv.TYPE:
                iValue = cb.readInt();
                tlv = new NexthopIPv4addressTlv(iValue);
                break;
            case NexthopIPv6addressTlv.TYPE:
                byte[] ipv6Value = new byte[NexthopIPv6addressTlv.VALUE_LENGTH];
                cb.readBytes(ipv6Value, 0, NexthopIPv6addressTlv.VALUE_LENGTH);
                tlv = new NexthopIPv6addressTlv(ipv6Value);
                break;
            case NexthopUnnumberedIPv4IDTlv.TYPE:
                tlv = NexthopUnnumberedIPv4IDTlv.read(cb);
                break;
            default:
                throw new PcepParseException("Unsupported TLV type :" + hType);
            }

            // Check for the padding
            int pad = hLength % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= cb.readableBytes()) {
                    cb.skipBytes(pad);
                }
            }

            llOutOptionalTlv.add(tlv);
        }

        if (0 < cb.readableBytes()) {

            throw new PcepParseException("Optional Tlv parsing error. Extra bytes received.");
        }
        return llOutOptionalTlv;
    }

    /**
     * Returns the writer index.
     *
     * @param cb of channel buffer.
     * @return writer index
     */
    protected int packOptionalTlv(ChannelBuffer cb) {

        ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();

        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();

            if (null == tlv) {
                log.debug("tlv is null from OptionalTlv list");
                continue;
            }
            tlv.write(cb);
        }
        return cb.writerIndex();
    }

    /**
     * Builder class for PCEP label object.
     */
    public static class Builder implements PcepLabelObject.Builder {

        private boolean bIsHeaderSet = false;
        private boolean bIsOFlagSet = false;
        private boolean bIsLabelSet = false;

        private PcepObjectHeader labelObjHeader;
        private boolean bOFlag;
        private int label;

        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepLabelObject build() throws PcepParseException {
            PcepObjectHeader labelObjHeader = this.bIsHeaderSet ? this.labelObjHeader : DEFAULT_LABEL_OBJECT_HEADER;
            boolean bOFlag = this.bIsOFlagSet ? this.bOFlag : DEFAULT_OFLAG;

            if (!this.bIsLabelSet) {
                throw new PcepParseException(" Label NOT Set while building PcepLabelObject.");
            }
            if (bIsPFlagSet) {
                labelObjHeader.setPFlag(bPFlag);
            }
            if (bIsIFlagSet) {
                labelObjHeader.setIFlag(bIFlag);
            }
            return new PcepLabelObjectVer1(labelObjHeader, bOFlag, this.label, this.llOptionalTlv);
        }

        @Override
        public PcepObjectHeader getLabelObjHeader() {
            return this.labelObjHeader;
        }

        @Override
        public Builder setLabelObjHeader(PcepObjectHeader obj) {
            this.labelObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public boolean getOFlag() {
            return this.bOFlag;
        }

        @Override
        public Builder setOFlag(boolean value) {
            this.bOFlag = value;
            this.bIsOFlagSet = true;
            return this;
        }

        @Override
        public int getLabel() {
            return this.label;
        }

        @Override
        public Builder setLabel(int value) {
            this.label = value;
            this.bIsLabelSet = true;
            return this;
        }

        @Override
        public LinkedList<PcepValueType> getOptionalTlv() {
            return this.llOptionalTlv;
        }

        @Override
        public Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv) {
            this.llOptionalTlv = llOptionalTlv;
            return this;
        }

        @Override
        public Builder setPFlag(boolean value) {
            this.bPFlag = value;
            this.bIsPFlagSet = true;
            return this;
        }

        @Override
        public Builder setIFlag(boolean value) {
            this.bIFlag = value;
            this.bIsIFlagSet = true;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("OFlag", bOFlag).add("label", label)
                .add("OptionalTlvList", llOptionalTlv).toString();
    }
}
