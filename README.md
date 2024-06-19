# NTAG 424 DNA Secure Dynamic Messaging ("SDM") Feature

This is the accompanying app to my articles **Demystify the Secure Dynamic Message with NTAG 424 DNA NFC tags (Android/Java) Parts 1 and 2**,
available here:

Part 1: https://medium.com/@androidcrypto/demystify-the-secure-dynamic-message-with-ntag-424-dna-nfc-tags-android-java-part-1-b947c482913c

Part 2: https://medium.com/@androidcrypto/demystify-the-secure-dynamic-message-with-ntag-424-dna-nfc-tags-android-java-part-2-1f8878faa928

## Overview

The NTAG 424 DNA tags are using a feature that was available only with the Mifare DESFire EV3 tags before.

Here is an excerpt from the datasheet: 
*Using AES-128 cryptography, the tag generates a unique NFC authentication message 
(SUN) each time it is being tapped. An NFC mobile device reads this tap-unique URL 
with the SUN authentication message, sends it to the host where tag and message 
authentication take place, and returns the verification result. The SUN authentication 
mechanism is working on Android without a dedicated application and from iOS11 
onwards using an application. This way, NTAG 424 DNA TT offers tag authentication, 
as well as data assurances on authenticity, integrity and even confidentiality, while also 
securing physical tag presence.*

In the documentation you will find two namings for the same feature:
- **Secure Dynamic Message** ("SDM")
- **Secure Unique Number** ("SUN")

## Technical informations about NTAG 424 DNA tags

In this document I'm always writing "NTAG 424 DNA" but there are "NTAG 424 DNA Tag Tamper" available as 
well. The SDM/SUN feature is working on both tag types.

NTAG 424 DNA datasheet: https://www.nxp.com/docs/en/data-sheet/NT4H2421Tx.pdf

NTAG 424 DNA and NTAG 424 DNA TagTamper features and hints: https://www.nxp.com/docs/en/application-note/AN12196.pdf

Symmetric Key Diversification AN10922: https://www.nxp.com/docs/en/application-note/AN10922.pdf

The tag has a predefined application and 3 predefined **Standard Data** files:
- **File 01h**: 32 bytes size, suitable for the "Capability Container" data (necessary for NDEF messages). The Communication mode is **Plain Communication**.
- **File 02h**: 256 bytes size, suitable for long NDEF messages. The Communication mode is **Plain Communication**.
- **File 03h**: 128 bytes size, suitable for protected data. The Communication mode is **Encrypted Communication**.

The application is setup with **5 application keys** that are of AES-128 size (meaning 16 bytes long). The default (fabric) keys are (in hex notation):
```plaintext
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```

The tags are using the **AES authentication** on default (fabric), but the authentication scheme can get changed to **LRP authentication** - this is an 
one-time-change that cannot get reversed.

This app can work with both authentication modes, but does not have an option to change the mode from AES to LRP (but the underlying library has a function
for this, see the documentation).

### Default content of file 1 (Capability Container)

NFC Forum Type 4 Tag Technical Specification: https://nfc-forum.org/uploads/specifications/97-NFCForum-TS-T4T-1.2.pdf

```plaintext
Capability Container File
The Capability Container (CC) file is a StandardData file with respect to access rights
management and data management. This file will hold the CC-file according to [14]. At
delivery it will hold following content:
• CCLEN = 0017h, i.e. 23 bytes
• T4T_VNo = 20h, i.e. Mapping Version 2.0
• MLe = 0100h, i.e. 256 bytes
• MLc = 00FFh, i.e. 255 bytes
• NDEF-File_Ctrl_TLV
– T = 04h, indicates the NDEF-File_Ctrl_TLV
– L = 06h, i.e. 6 bytes
– NDEF-File File Identifier = E104h
– NDEF-File File Size = 0100h, i.e. 256 bytes
– NDEF-File READ Access Condition = 00h, i.e. READ access granted without any security
– NDEF-File WRITE Access Condition = 00h, i.e. WRITE access granted without any security
  or NDEF-File WRITE Access Condition = FFh, i.e. NO WRITE access, Read Only
```

This content is written to the  file 1:

```plaintext
Original CC with Read and Write Access
byte[] NDEF_FILE_01_CAPABILITY_CONTAINER = Utils.hexStringToByteArray("000F20003A00340406E10401000000");

Modified CC with Read Only Access
byte[] NDEF_FILE_01_CAPABILITY_CONTAINER = Utils.hexStringToByteArray("000F20003A00340406E104010000FF");
```

### NDEF URL-template (File 02)

This URL is written in File 02h as template for a SUN/SDM message with **Plain Tag UID** and **Plain Read Counter**, 
additional the **CMAC** of the data.

```plaintext
https://sdm.nfcdeveloper.com/tagpt?uid=**************&ctr=******&cmac=****************
```

When the prepared tag is tapped to a NFC reader (on a smartphone) the system will recognise that the NDEF data are an URL, 
so the system will forward the data to a browser and open the website **https://sdm.nfcdeveloper.com/tagpt**. As you can see 
the parameter "uid", "ctr" and "cmac" are filled with placeholders ("**..."), so the "Secure Dynamic Messaging Backend Server Demo" 
is giving an error message ("400 Bad Request: Failed to decode parameters").

If the SUN/SDM feature is enabled you will get the "real" tag data and a positive test result.

## Example for a NDEF URL-Link with Plaintext data (UID and Read Counter):
```plaintext
https://sdm.nfcdeveloper.com/tagpt?uid=049F50824F1390&ctr=000001&cmac=2446E527C37E073A
```

Validation at the backend server:
```plaintext
Secure Dynamic Messaging Backend Server Demo
Cryptographic signature validated.
Encryption mode: AES
NFC TAG UID: 049f50824f1390
Read counter: 1
```

## Example for a NDEF URL-Link with Encrypted PICC data (UID and Read Counter):
```plaintext
https://sdm.nfcdeveloper.com/tag?picc_data=EF963FF7828658A599F3041510671E88&cmac=94EED9EE65337086
```

Validation at the backend server:
```plaintext
Secure Dynamic Messaging Backend Server Demo
Cryptographic signature validated.
Encryption mode: AES
PICC Data Tag: c7
NFC TAG UID: 04de5f1eacc040
Read counter: 61
```

## Example for a NDEF URL-Link with Encrypted PICC data (UID and Read Counter) and Encrypted File data:
```plaintext
https://sdm.nfcdeveloper.com/tag?picc_data=4E8D0223F8C17CDCCE5BC24076CFAA0D&enc=B56FED7FF7B23791C0684F17E117C97450723BB5C104E809C8929F0264CB99F9969D07FC32BB2D11995AEF826E355097&cmac=5FD76DE4BD942DFC
```

Validation at the backend server:
```plaintext
Secure Dynamic Messaging Backend Server Demo
Cryptographic signature validated.
Encryption mode: AES
PICC Data Tag: c7
NFC TAG UID: 049f50824f1390
Read counter: 16
File data (hex): 31392e30352e323032342031323a32323a333323313233342a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a
File data (UTF-8): 19.05.2024 12:22:33#1234************************
```

Btw.: the named website is run by "Arx Research, Inc.". The source code is available here: https://github.com/nfc-developer/sdm-backend.

### Default file access rights (fabric settings)

| **File Nr** | **Read Access** | **Write Access** | **Read & Write Access** |
|:-----------:|:---------------:|:----------------:|:-----------------------:|
|  File 01h   |       Eh        |        0h        |           0h            |
|  File 02h   |       Eh        |        Eh        |           Eh            |
|  File 03h   |       2h        |        3h        |           3h            |

### Modified file access rights

| **File Nr** | **Read Access** | **Write Access** | **Read & Write Access** |
|:-----------:|:---------------:|:----------------:|:-----------------------:|
|  File 01h   |       Eh        |        0h        |           0h            |
|  File 02h   |       Eh        |        1h        |           1h            |
|  File 03h   |       2h        |        3h        |           3h            |

**SDM Meta Read Access Key**: 03h (used for Encryption of PICC data)

**SDM File Read Access Key**: 04h (used for Encryption of File data and CMAC calculation)

**SDM Counter Retrieve Access Key**: 04h

## About this app:

It is developed using Android Studio version Jellyfish | 2023.3.1 Patch 2 and is running on SDK 21 to 34 (Android 14) (tested on
Android 8, 9 and 13 with real devices).

Some notes on typical sessions with the card: I recommend that you lay your phone on the tag and after the connection don't move the phone to hold the
connection.

## Ready to use compiled and build debug app

A ready to use app in DEBUG mode is available under the debug folder.

## Dependencies

This app is based on the **NXP NTAG 424 DNA Library**, developed by **Jonathan Bartlett**. 
The full code is available here: https://github.com/johnnyb/ntag424-java. As the library is 
using a MIT license this app is under the **MIT license** as well.

## NDEF Reader Examples

```plaintext
All examples are for AES encryption scheme

Plaintext UID + Counter:
NDEF message: https://sdm.nfcdeveloper.com/tagpt?uid=049F50824F1390&ctr=000001&cmac=2446E527C37E073A
UID:049F50824F1390
Counter:000001
CMAC:2446E527C37E073A
The CMAC is VALIDATED (AES)

Plaintext UID:
NDEF message: https://sdm.nfcdeveloper.com/tagpt?uid=049F50824F1390&cmac=B1EE1FD5DC0D9654
UID:049F50824F1390
Counter:
CMAC:B1EE1FD5DC0D9654
The CMAC is VALIDATED (AES)

Plaintext Counter: 
NDEF message: https://sdm.nfcdeveloper.com/tagpt?ctr=000003&cmac=C288EB1DF43C6A78
UID:
Counter:000003
CMAC:C288EB1DF43C6A78
The CMAC is VALIDATED (AES)

Encrypted PICC data UID + Counter:
NDEF message: https://sdm.nfcdeveloper.com/tag?picc_data=DF33C65555C7BD93FBD5BF32811FE51C&cmac=E1EB1235588A4E74
PICC:DF33C65555C7BD93FBD5BF32811FE51C
UID:049f50824f1390
Counter:4
CMAC:E1EB1235588A4E74
The CMAC is VALIDATED

Encrypted PICC data UID:
NDEF message: https://sdm.nfcdeveloper.com/tag?picc_data=BFA4ECDA959BB0B6A05A594250E9C22A&cmac=B1EE1FD5DC0D9654
PICC:BFA4ECDA959BB0B6A05A594250E9C22A
UID:049f50824f1390
Counter:0
CMAC:B1EE1FD5DC0D9654
The CMAC is VALIDATED

Encrypted PICC data Counter:
NDEF message: https://sdm.nfcdeveloper.com/tag?picc_data=6AE2FC322BDA1DEE03C0141CBC6F5180&cmac=8415EA4B86F5D7D5
PICC:6AE2FC322BDA1DEE03C0141CBC6F5180
UID:
Counter:5
CMAC:8415EA4B86F5D7D5
The CMAC is VALIDATED

Encrypted PICC data UID + Counter and encrypted File data:
NDEF message: https://sdm.nfcdeveloper.com/tag?picc_data=A891203194DA2DAA219FE68290EC52CA&enc=4ED043CDA1771D47B87AE219BAC52D5B4653D92D1B5A987E7A218D1F82CFA5B3&cmac=EA18B774C93F34AE
PICC:A891203194DA2DAA219FE68290EC52CA
UID:049f50824f1390
Counter:6
CMAC:EA18B774C93F34AE
Encrypted File Data:
4ED043CDA1771D47B87AE219BAC52D5B4653D92D1B5A987E7A218D1F82CFA5B3
Decrypted File data:
26.05.2024 23:44:56#1234********
cmacCalc length: 8 data: ea18b774c93f34ae
The CMAC is VALIDATED

Encrypted PICC data UID + Counter and encrypted File data (here I'm using the DEFAULT keys but not the CUSTOM keys):
NDEF message: https://sdm.nfcdeveloper.com/tag?picc_data=E483814B288D504257C04520287C3DBA&enc=193B0831AD6A3C0CDE8F22E8720389E015C5C7CB7BE71975F2DE02A0013B9FB9&cmac=6D2D5671D902213E
PICC:E483814B288D504257C04520287C3DBA
UID:5758994b5a7352
Counter:9931778
CMAC:6D2D5671D902213E
Encrypted File Data:
193B0831AD6A3C0CDE8F22E8720389E015C5C7CB7BE71975F2DE02A0013B9FB9
Decrypted File data:
ql!$Yi����p�;q�h�\�Q2�a��o-�h�
cmacCalc length: 8 data: 58ed8583800cc144
The CMAC is VOID

Encrypted PICC data UID + Counter and encrypted File data (here I'm using the CUSTOM keys):
NDEF message: https://sdm.nfcdeveloper.com/tag?picc_data=4818429EDACA68F1CECC481548C7F3E9&enc=D48C419E1183A5E2918B212F711FB790229D4CEE2BB3991F44D54EEE5E2A92A7&cmac=F5CBFBFDE04D4548
PICC:4818429EDACA68F1CECC481548C7F3E9
UID:049f50824f1390
Counter:8
CMAC:F5CBFBFDE04D4548
Encrypted File Data:
D48C419E1183A5E2918B212F711FB790229D4CEE2BB3991F44D54EEE5E2A92A7
Decrypted File data:
26.05.2024 23:46:41#1234********
cmacCalc length: 8 data: f5cbfbfde04d4548
The CMAC is VALIDATED
```

Tag Overview after "Encrypted PICC data UID + Counter and encrypted File data using the CUSTOM keys"
```plaintext
============================
Authentication with FACTORY ACCESS_KEY 0
AES Authentication SUCCESS
----------------------------
App Key 1 is FACTORY key
App Key 2 is FACTORY key
App Key 3 is CUSTOM key
App Key 4 is CUSTOM key
============================
Get the File Settings
= FileSettings =
fileNumber: 1
fileType: n/a
commMode: PLAIN
accessRights RW:       0
accessRights CAR:      0
accessRights R:        14
accessRights W:        0
fileSize: 32
= Secure Dynamic Messaging =
isSdmEnabled: false
isSdmOptionUid: false
isSdmOptionReadCounter: true
isSdmOptionReadCounterLimit: false
isSdmOptionEncryptFileData: false
isSdmOptionUseAscii: true
sdmMetaReadPerm:             14
sdmFileReadPerm:             14
sdmReadCounterRetrievalPerm: 14
----------------------------
= FileSettings =
fileNumber: 2
fileType: n/a
commMode: PLAIN
accessRights RW:       2
accessRights CAR:      0
accessRights R:        14
accessRights W:        2
fileSize: 256
= Secure Dynamic Messaging =
isSdmEnabled: true
isSdmOptionUid: true
isSdmOptionReadCounter: true
isSdmOptionReadCounterLimit: false
isSdmOptionEncryptFileData: true
isSdmOptionUseAscii: true
sdmMetaReadPerm:             15
sdmFileReadPerm:             15
sdmReadCounterRetrievalPerm: 4
----------------------------
= FileSettings =
fileNumber: 3
fileType: n/a
commMode: FULL
accessRights RW:       3
accessRights CAR:      2
accessRights R:        0
accessRights W:        3
fileSize: 128
= Secure Dynamic Messaging =
isSdmEnabled: false
isSdmOptionUid: false
isSdmOptionReadCounter: true
isSdmOptionReadCounterLimit: false
isSdmOptionEncryptFileData: false
isSdmOptionUseAscii: true
sdmMetaReadPerm:             14
sdmFileReadPerm:             14
sdmReadCounterRetrievalPerm: 14
============================
content of file 01 length: 32 data: 000f20003a00340406e104010000ff060d5be8466c63ca49e58a22a62988ace3
----------------------------
content of file 02 length: 256 data: 00abd101a7550068747470733a2f2f73646d2e6e6663646576656c6f7065722e636f6d2f7461673f706963635f646174613d2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a26656e633d32362e30352e323032342032333a34363a343123313233342a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a26636d61633d2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2ad1f1d27d7d505e24f5a05a7f8cca0b1b01e8355c873dcf1ed74abfb14a620edd200c98d9790fa5d214ea94565cf89f5d66ee1c6511c5acc5fb1918fa3f55a32b77adead939aabce405df1e22c62b7add23c017
Communicator: BytesReceived: 7856B8214336102F8F269C2E15715777FD8AABDD5D7786D652EBB29469F5983237C2702CDAC402ABE72C0974A2942AB75C77323378EF96BB97D14CB39A8760403C0DB61FA4770069EF31C5622DE1A84E0F2E9350627726FFFF1E5EB62710ADCFF59C73AE02F1411D08C6614F71887D36A80C1D42D1E3071A021A410F057D08E4E3D1C4120461DF757182D16E69EEB323A427E05F0FC695129100
Communicator: Decrypted data: 000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F202122232425262728292A2B2C2D2E2F303132333435363738393A3B3C3D3E3F404142434445464748494A4B4C4D4E4F505152535455565758595A5B5C5D5E5F606162636465666768696A6B6C6D6E6F707172737475767778797A7B7C7D7E7F80000000000000000000000000000000
ASCII Data: ??���U??https://sdm.nfcdeveloper.com/tag?picc_data=********************************&enc=26.05.2024 23:46:41#1234****************************************&cmac=****************���}}P^$��Z���5\�=��J��Jb� ��y���V\��]f�eŬ���?U�+w���9����"�+z�#�
----------------------------
content of file 03 length: 144 data: 000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f80000000000000000000000000000000
----------------------------
```
