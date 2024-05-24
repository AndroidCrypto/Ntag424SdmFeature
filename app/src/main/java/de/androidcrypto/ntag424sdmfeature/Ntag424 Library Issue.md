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

19.05.2024 Answer Jon: 
```plaintext
This is actually working correctly. The problem is that the MAC starts at the earlier of MacInputOffset or EncOffset, but utilizes the entirety of the string all the way to the MacOffset, and in its original (encrypted) form.
The code to generate the CMAC would be:
decryptedPiccDataTest.performShortCMAC("B56FED7FF7B23791C0684F17E117C97450723BB5C104E809C8929F0264CB99F9969D07FC32BB2D11995AEF826E355097&cmac=".getBytes(StandardCharsets.UTF_8));
Note that even the "&cmac=" is part of the MAC input.
```

20.05.2024 closure by me:

Dear Jon,
Thanks for your fast answer and help. Your "workflow" is working like expected, below is my edited
test program for others who are struggling like me.

Greetings
Michael

```plaintext
byte[] encryptedPiccDataTest = Utils.hexStringToByteArray("4E8D0223F8C17CDCCE5BC24076CFAA0D");    
String encryptedFileDataStringTest = "B56FED7FF7B23791C0684F17E117C97450723BB5C104E809C8929F0264CB99F9969D07FC32BB2D11995AEF826E355097";
byte[] encryptedFileDataTest = Utils.hexStringToByteArray(encryptedFileDataStringTest);                      
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
// We need to use the 'encrypted file data' including the following '&cmac=' as input
// for the CMAC calculation. Then we have to convert this string into byte[] representation
byte[] cmacDataTest = (encryptedFileDataStringTest + "&cmac=").getBytes(StandardCharsets.UTF_8);                                                                            
System.out.println("CMAC expected  : " + Utils.bytesToHex(cmacTest));                                               
System.out.println("CMAC calculated: " + Utils.bytesToHex(cmacCalcTest));                                                   
System.out.println("The CMAC is validated: " + Arrays.equals(cmacCalcTest, cmacTest));                                  
/*                                                                                                                      
CMAC expected  : 5fd76de4bd942dfc                                                                                   
CMAC calculated: 5fd76de4bd942dfc                                                                                   
The CMAC is validated: true                                                                                        
*/   
```


Other issue regarding Encrypted PICC data (UID only) + encrypted File data


```plaintext
Encrypted PICC (UID + Read Counter) + File Data (success)
Communicator: Data before encryption: 4020E2D1FF223200005700005700004000009D0000
Communicator: BytesSending: 905F000029020767DA18BD8BFEDF2C099A9535C5360E7E92C1ED5DED993D4B1F4A1D841738A29FB2A2159F90969200
Communicator: BytesReceived: D43D4BF9107CD21F9100

Encrypted PICC (UID only) + File Data (fails)
Communicator: Data before encryption: 4020E291FF223200005700005700004000009D0000
Communicator: BytesSending: 905F000029020A56843EB510FB259509E8A617800903DF5308B7BF3812A6F15B75D11F3027A8A2C0F52A22596A0B00
Communicator: BytesReceived: 919E
Communicator: Decrypted data:
net.bplearning.ntag424.exception.ProtocolException: Invalid status result: 919E

Difference in data send is here:
Communicator: Data before encryption: 4020E2 D1 FF223200005700005700004000009D0000 (success)
Communicator: Data before encryption: 4020E2 91 FF223200005700005700004000009D0000 (failure)
                                             |
                                      |         40h = File Option
                                        |       20h = Access Rights Part 1
                                           |    E2h = Access Rights Part 2
                                             |  SDM Options
                                                Bit 7: 1 = UID enbled
                                                Bit 6: 1 = Read Counter enabled
                                                Bit 5: 1 = Read Counter Limit enabled                                              
                                                Bit 4: 1 = SDMENCFileData enabled
                                                Bit 3: 0 = RFU
                                                Bit 2: 0 = RFU
                                                Bit 1: 0 = RFU
                                                Bit 0: 1 = Encoding mode ASCII enabled
                                                For SDM with UID, File Data + ASCII we need to set the bits 0, 4 + 7 = 91h
                                                
```


