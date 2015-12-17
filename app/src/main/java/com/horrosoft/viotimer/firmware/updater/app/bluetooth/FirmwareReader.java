package com.horrosoft.viotimer.firmware.updater.app.bluetooth;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alexey on 16.12.2015.
 *
 */
public class FirmwareReader {
    public class PacketSetting {
        public String okResponse;
        public String badResponse;
        public int retryCount = -1;
        public int timeout = 3;
        public int delayBetweenResendPacket = 3;
    }

    private InputStream inputStream;
    private boolean correct;

    private PacketSetting firstPacket = new PacketSetting();
    private PacketSetting middlePacket = new PacketSetting();
    private PacketSetting lastPacket = new PacketSetting();
    private int baudrate = -1;
    private int packetLength = -1;
    private String description = "There is no description of the firmware file.";

    private XmlPullParser myParser;

    private byte[] firmwareData;

    private static final String firstPacketTag = "first_packet";
    private static final String middlePacketTag = "middle_packet";
    private static final String lastPacketTag = "last_packet";

    private static final String baudrateTag = "baudrate";
    private static final String packetLengthTag = "packet_length";
    private static final String okResponseTag = "ok_response";
    private static final String errorResponseTag = "error_response";
    private static final String retryCountTag = "retry_count";
    private static final String timeoutTag = "timeout";
    private static final String delayBetweenResendPacketTag = "delay_between_resend_packet";

    private static final String descriptionTag = "description";

    // private static final String Tag = "";

    public PacketSetting getFirstPacket() {
        return firstPacket;
    }

    public PacketSetting getMiddlePacket() {
        return middlePacket;
    }

    public PacketSetting getLastPacket() {
        return lastPacket;
    }

    public int getPacketLength() {
        return packetLength;
    }

    public int getBaudrate() {
        return baudrate;
    }

    public int totalParts() {
        if (correct) {
            return firmwareData.length / packetLength;
        }
        return 0;
    }

    public String getDescription() {
        return description;
    }

    public boolean isValid() {
        return correct;
    }

    public byte[] getPacket(int packetNum) {
        byte[] retVal = null;
        if (totalParts() > packetNum) {
            int offset = packetNum * packetLength;
            retVal = new byte[packetLength];
            System.arraycopy(firmwareData, offset, retVal, 0, packetLength);
        }
        return retVal;
    }


    public FirmwareReader(byte[] binaryData) {
        this.correct = true;
        try {
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            this.myParser = xmlFactoryObject.newPullParser();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            correct = false;
        }
        if (correct) {
            inputStream = new ByteArrayInputStream(binaryData);
            if (readXmlPart()) {
                readFirmwareData();
            }
            inputStream = null;
        }
    }

    private void readFirmwareData() {
        int firmwareLen;
        try {
            firmwareLen = inputStream.available();
            firmwareData = new byte[firmwareLen];
            int read = inputStream.read(firmwareData);
            if (read >= 0 && read != firmwareLen) {
                correct = false;
            }
        } catch (IOException e) {
            correct = false;
            e.printStackTrace();
        }
        if (!correct) {
            firmwareData = null;
        }
    }

    private boolean checkPacketSetting(PacketSetting packetSetting) {
        if (packetSetting.okResponse.isEmpty()) {
            return false;
        } else if (packetSetting.badResponse.isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean readXmlPart() {
        List<Byte> list = new ArrayList<Byte>();
        while (true) {
            try {
                int b = inputStream.read();
                if (b < 0) {
                    correct = false;
                    break;
                } else if (b > 0) {
                    list.add((byte)b);
                } else {
                    break;
                }
            } catch (IOException e) {
                correct = false;
                break;
            }
        }
        if (correct) {
            byte[] xmlData = new byte[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                xmlData[i] = list.get(i);
            }
            InputStream xmlStream = new ByteArrayInputStream(xmlData);
            try {
                myParser.setInput(xmlStream, null);
                int event = myParser.getEventType();
                String text;
                PacketSetting currentPacket = null;
                String name = "";
                while (event != XmlPullParser.END_DOCUMENT) {

                    switch (event) {
                        case XmlPullParser.START_TAG: {
                            name = myParser.getName();
                            if (name.equals(firstPacketTag)) {
                                currentPacket = firstPacket;
                            } else if (name.equals(middlePacketTag)) {
                                currentPacket = middlePacket;
                            } else if (name.equals(lastPacketTag)) {
                                currentPacket = lastPacket;
                            }
                        }
                        break;
                        case XmlPullParser.END_TAG: {
                            if (name.equals(firstPacketTag) || name.equals(middlePacketTag) || name.equals(lastPacketTag)) {
                                currentPacket = null;
                            }
                            name = "";
                        }
                        break;
                        case XmlPullParser.TEXT: {
                            text = myParser.getText();
                            if (currentPacket != null) {
                                if (name.equals(okResponseTag)) {
                                    currentPacket.okResponse = text;
                                } else if (name.equals(errorResponseTag)) {
                                    currentPacket.badResponse = text;
                                } else if (name.equals(retryCountTag)) {
                                    currentPacket.retryCount = Integer.parseInt(text);
                                } else if (name.equals(timeoutTag)) {
                                    currentPacket.timeout = Integer.parseInt(text);
                                } else if (name.equals(delayBetweenResendPacketTag)) {
                                    currentPacket.delayBetweenResendPacket = Integer.parseInt(text);
                                }
                            }
                            if (name.equals(baudrateTag)) {
                                baudrate = Integer.parseInt(text);
                            } else if (name.equals(packetLengthTag)) {
                                packetLength = Integer.parseInt(text);
                            } else if (name.equals(descriptionTag)) {
                                description = text;
                            }
                        }
                        break;

                    }
                    event = myParser.next();
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                correct = false;
            } catch (IOException e) {
                e.printStackTrace();
                correct = false;
            }
        }
        if (correct) {
            correct = false;
            do {
                if (baudrate < 0) {
                    break;
                } else if (packetLength < 0) {
                    break;
                } else if (!checkPacketSetting(firstPacket)) {
                    break;
                } else if (!checkPacketSetting(middlePacket)) {
                    break;
                } else if (!checkPacketSetting(lastPacket)) {
                    break;
                }
                correct = true;
            } while (false);
        }
        return correct;
    }
}
