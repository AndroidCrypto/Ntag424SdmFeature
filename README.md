# NTAG 424 DNA Secure Unique Number ("SUN") Feature

## Project Status: Unfinished


## Technical information's about NTAG 424 DNA tags



NTAG 424 DNA datasheet: https://www.nxp.com/docs/en/data-sheet/NT4H2421Tx.pdf

NTAG 424 DNA and NTAG 424 DNA TagTamper features and hints: https://www.nxp.com/docs/en/application-note/AN12196.pdf

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

### Default file access rights

|:--:|:-----|:---:|
| File | Read Access | Write Access |






## Dependencies

This app is based on the **NXP NTAG 424 DNA Library**, developed by **Jonathan Bartlett**. 
The full code is available here: https://github.com/johnnyb/ntag424-java. As the library is 
using a MIT license this app is under the **MIT license** as well.

