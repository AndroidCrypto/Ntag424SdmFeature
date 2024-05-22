package de.androidcrypto.ntag424sunfeature;

import net.bplearning.ntag424.command.FileSettings;

/**
 * This class dumps the contents of the FileSettings class in a human readable form
 */

public class DnacFileSettingsDumper {

        public static String run(int fileNumber, FileSettings fs) {
            StringBuilder sb = new StringBuilder();
            sb.append("= FileSettings =").append("\n");
            sb.append("fileNumber: ").append(fileNumber).append("\n");
            sb.append("fileType: ").append("n/a").append("\n"); // todo expose get file type for DESFire
            if (fs == null) {
                sb.append("FileSettings are NULL").append("\n");
            } else {
                sb.append("commMode: ").append(fs.commMode.toString()).append("\n");
                sb.append("accessRights RW:       ").append(fs.readWritePerm).append("\n");
                sb.append("accessRights CAR:      ").append(fs.changePerm).append("\n");
                sb.append("accessRights R:        ").append(fs.readPerm).append("\n");
                sb.append("accessRights W:        ").append(fs.writePerm).append("\n");
                sb.append("fileSize: ").append(fs.fileSize).append("\n");
                sb.append("= Secure Dynamic Messaging =").append("\n");
                sb.append("isSdmEnabled: ").append(fs.sdmSettings.sdmEnabled).append("\n");
                sb.append("isSdmOptionUid: ").append(fs.sdmSettings.sdmOptionUid).append("\n");
                sb.append("isSdmOptionReadCounter: ").append(fs.sdmSettings.sdmOptionReadCounter).append("\n");
                sb.append("isSdmOptionReadCounterLimit: ").append(fs.sdmSettings.sdmOptionReadCounterLimit).append("\n");
                sb.append("isSdmOptionEncryptFileData: ").append(fs.sdmSettings.sdmOptionEncryptFileData).append("\n");
                sb.append("isSdmOptionUseAscii: ").append(fs.sdmSettings.sdmOptionUseAscii).append("\n");
                sb.append("sdmMetaReadPerm:             ").append(fs.sdmSettings.sdmMetaReadPerm).append("\n");
                sb.append("sdmFileReadPerm:             ").append(fs.sdmSettings.sdmFileReadPerm).append("\n");
                sb.append("sdmReadCounterRetrievalPerm: ").append(fs.sdmSettings.sdmReadCounterRetrievalPerm).append("\n");
/*
                sb.append("sdmUidOffset:         ").append(fs.sdmSettings.sdmUidOffset).append("\n");
                sb.append("sdmUidOffset:         ").append(fs.sdmSettings.sdmUidOffset).append("\n");
                sb.append("sdmUidOffset:         ").append(fs.sdmSettings.sdmUidOffset).append("\n");
                sb.append("sdmReadCounterOffset: ").append(fs.sdmSettings.sdmReadCounterOffset).append("\n");
                sb.append("sdmPiccDataOffset:    ").append(fs.sdmSettings.sdmPiccDataOffset).append("\n");
                sb.append("sdmMacInputOffset:    ").append(fs.sdmSettings.sdmMacInputOffset).append("\n");
                sb.append("sdmMacOffset:         ").append(fs.sdmSettings.sdmMacOffset).append("\n");
                sb.append("sdmEncOffset:         ").append(fs.sdmSettings.sdmEncOffset).append("\n");
                sb.append("sdmEncLength:         ").append(fs.sdmSettings.sdmEncLength).append("\n");
                sb.append("sdmReadCounterLimit:  ").append(fs.sdmSettings.sdmReadCounterLimit).append("\n");
 */
            }
            return sb.toString();
        }

    public static String runPermissions(int fileNumber, FileSettings fs) {
        StringBuilder sb = new StringBuilder();
        sb.append("fNr: ").append(fileNumber);
        if (fs == null) {
            sb.append("\n").append("FileSettings are NULL").append("\n");
        } else {
            sb.append(" size: ").append(String.format("%03d", fs.fileSize));
            sb.append(" commMode: ").append(fs.commMode.toString());
            sb.append(" AR RW: ").append(fs.readWritePerm);
            sb.append(" CAR: ").append(fs.changePerm);
            sb.append(" R: ").append(fs.readPerm);
            sb.append(" W: ").append(fs.writePerm);
            sb.append(" sdmEnabled: ").append(fs.sdmSettings.sdmEnabled);
            sb.append(" sdmMetaRead: ").append(fs.sdmSettings.sdmMetaReadPerm);
            sb.append(" sdmFileRead: ").append(fs.sdmSettings.sdmFileReadPerm);
            sb.append(" sdmReadCounterRetrieval: ").append(fs.sdmSettings.sdmReadCounterRetrievalPerm);
        }
        return sb.toString();
    }
}

/*
fileSettings of factory settings tag:
fileNumber: 1
received  <-- length: 9 data: 000000e02000009100

fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: 00
accessRights R | W: E0
accessRights RW:  0
accessRights CAR: 0
accessRights R:   14
accessRights W:   0
fileSize: 32

fileNumber: 2
received  <-- length: 9 data: 0000e0ee0001009100

fileType: 0 (Standard)
communicationSettings: 00 (Plain)
accessRights RW | CAR: E0
accessRights R | W: EE
accessRights RW:  14
accessRights CAR: 0
accessRights R:   14
accessRights W:   14
fileSize: 256

fileNumber: 3
received  <-- length: 9 data: 000330238000009100

fileType: 0 (Standard)
communicationSettings: 03 (Encrypted)
accessRights RW | CAR: 30
accessRights R | W: 23
accessRights RW:  3
accessRights CAR: 0
accessRights R:   2
accessRights W:   3
fileSize: 128

 */
