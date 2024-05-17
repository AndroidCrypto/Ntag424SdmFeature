# NTAG 424 DNA Secure Unique Number ("SUN") Feature

## Project Status: Unfinished

The NTAG 424 DNA tag are using a feature that was available only with the Mifare DESFire EV3 tags.

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
- Secure Unique Number ("SUN")
- Secure Dynamic Message ("SDM")

## Technical information's about NTAG 424 DNA tags

In this document I'm always writing "NTAG 424 DNA" but there are "NTAG 424 DNA Tag Tamper" available as 
well. The SUN/SDM feature is working on both tag types.

NTAG 424 DNA datasheet: https://www.nxp.com/docs/en/data-sheet/NT4H2421Tx.pdf

NTAG 424 DNA and NTAG 424 DNA TagTamper features and hints: https://www.nxp.com/docs/en/application-note/AN12196.pdf

The tag has a predefined application and 3 predefined Standard Data files:
- File 01h: 32 bytes size, suitable for the "Capability Container" data (necessary for NDEF messages). The Communication mode is **Plain Communication**.
- File 02h: 256 bytes size, suitable for long NDEF messages. The Communication mode is **Plain Communication**.
- File 03h: 128 bytes size, suitable for protected data. The Communication mode is **Encrypted Communication**.

The application is setup with **5 application keys** that are of AES-128 size (meaning 16 bytes). The default (fabric) keys are (in hex notation):
```plaintext
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
```

The tags are using the **AES authentication** on default (fabric), but the authentication scheme can get changed to **LRP authentication** - this is an 
one-time-change that cannot get reversed.

This app can work with both authentication modes, but does not have an option to change the mode from AES to LRP.

### Default content of file 1 (Capability Container)

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
```

This content is written to the  file 1:

```plaintext

byte[] NDEF_FILE_01_CAPABILITY_CONTAINER = Utils.hexStringToByteArray("001720010000ff0406E10401000000");

byte[] NDEF_FILE_01_CAPABILITY_CONTAINER = Utils.hexStringToByteArray("000F20003A00340406E10401000000");
```

### NDEF URL-template (File 02)

This URL is written in File 02h as template for a SUN/SDM message with **Plain Tag UID** and **Plain Read Counter**, 
additional the **CMAC** of the data.

```plaintext
https://sdm.nfcdeveloper.com/tagpt?uid=**************&ctr=******&cmac=****************
```

When the prepared tag is tapped to a NFC reader (on a smartphone) the system will recognise that the NDEF data are an URL, 
so the system will forward the data to a browser and open the website **https://sdm.nfcdeveloper.com/tagpt**. As you can see 
the parameter "uid", "ctr" and "cmac" are filled with placeholders ("**...") so the "Secure Dynamic Messaging Backend Server Demo" 
is giving an error message ("400 Bad Request: Failed to decode parameters").

If the SUN/SDM feature is enabled you will get the "real" tag data and a positive test result.

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

**SDM Meta Read Access Key**: 02h

**SDM File Read Access Key**: 03h

**SDM Counter Retrieve Access Key**: 04h

## Dependencies

This app is based on the **NXP NTAG 424 DNA Library**, developed by **Jonathan Bartlett**. 
The full code is available here: https://github.com/johnnyb/ntag424-java. As the library is 
using a MIT license this app is under the **MIT license** as well.

