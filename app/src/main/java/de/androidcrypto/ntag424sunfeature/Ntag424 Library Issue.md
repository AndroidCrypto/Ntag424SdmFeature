# NTAG 424 DNA Library issue

19.05.2024 https://github.com/johnnyb/ntag424-java/issues/9

Validation of the CMAC fails with encrypted PICC and file data

Dear Jon,
There seems to be an issue when validating a NDEF message with encrypted PICC data and encrypted file data.

I setup the tag with your library and this is the example URL my NTAG 424 DNA tag generated and the backend server is 
validating:

https://sdm.nfcdeveloper.com/tag?picc_data=4E8D0223F8C17CDCCE5BC24076CFAA0D&enc=B56FED7FF7B23791C0684F17E117C97450723BB5C104E809C8929F0264CB99F9969D07FC32BB2D11995AEF826E355097&cmac=5FD76DE4BD942DFC

The result is as follows:

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

I'm using this code to decrypt the PICC data and encrypted file data, the keys are the default AES-128 keys (16 * 00h), 
the authentication is AES (not LRP):

```plaintext
byte[] encryptedPiccDataTest = Utils.hexStringToByteArray("4E8D0223F8C17CDCCE5BC24076CFAA0D");                          
byte[] encryptedFileDataTest = Utils.hexStringToByteArray("B56FED7FF7B23791C0684F17E117C97450723BB5C104E809C8929F0264CB9
byte[] cmacTest = Utils.hexStringToByteArray("5FD76DE4BD942DFC");

// step 1: PICC data decryption                                                                                         
PiccData decryptedPiccDataTest = PiccData.decodeFromEncryptedBytes(encryptedPiccDataTest, new byte[16], false);         
byte[] uidDecryptedTest = decryptedPiccDataTest.getUid();                                                               
int readCounterDecryptedTest = decryptedPiccDataTest.getReadCounter();                                                  
System.out.println("UID: " + Utils.bytesToHex(uidDecryptedTest));                                                       
System.out.println("ReadCounter: " + readCounterDecryptedTest);

// step 2: decrypt Encrypted File data                                                                                  
decryptedPiccDataTest.setMacFileKey(new byte[16]);                                                                      
byte[] decryptedFileDataTest = decryptedPiccDataTest.decryptFileData(encryptedFileDataTest);                            
System.out.println("decryptedFileData: " + Utils.bytesToHex(decryptedFileDataTest));                                    
System.out.println("decryptedFileData: " + new String(decryptedFileDataTest, StandardCharsets.UTF_8));                  
/*                                                                                                                      
UID: 049f50824f1390                                                                                                 
ReadCounter: 16                                                                                                     
decryptedFileData: 31392e30352e323032342031323a32323a333323313233342a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a2a
decryptedFileData: 19.05.2024 12:22:33#1234************************                                                 
*/

// step 3: validate the CMAC                                                                                            
byte[] cmacCalcTest = decryptedPiccDataTest.performShortCMAC(decryptedFileDataTest);                                    
System.out.println("CMAC expected  : " + Utils.bytesToHex(cmacCalcTest));                                               
System.out.println("CMAC calculated: " + Utils.bytesToHex(cmacTest));                                                   
System.out.println("The CMAC is validated: " + Arrays.equals(cmacCalcTest, cmacTest));                                  
/*                                                                                                                      
CMAC expected  : 93bad3028afb5d0c                                                                                   
CMAC calculated: 5fd76de4bd942dfc                                                                                   
The CMAC is validated: false                                                                                        
*/                                                                                                                     
```

The validation is working with Plaintext and Encrypted PICC data without any problems.

Greetings
Michael