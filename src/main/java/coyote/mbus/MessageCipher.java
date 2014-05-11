/*
 * Copyright (c) 2006 Stephan D. Cote' - All rights reserved.
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the MIT License which accompanies this distribution, and is 
 * available at http://creativecommons.org/licenses/MIT/
 *
 * Contributors:
 *   Stephan D. Cote 
 *      - Initial concept and initial implementation
 */
package coyote.mbus;

import java.security.KeyException;
import java.util.Date;

import coyote.commons.ByteUtil;


/**
 * MessageCipher is an implementation of the Blowfish 64-bit symmetric block 
 * cipher with a variable length key, up to 448-bit or 56 bytes, designed by 
 * Bruce Schneier. 
 * 
 * <p>The Blowfish algorithm is a symmetric block cipher that can be used as a 
 * drop-in replacement for DES or IDEA. It takes a variable-length key, from 32 
 * bits to 448 bits, making it ideal for both domestic and exportable use.</p>
 * 
 * <h3>Blowfish Encryption</h3>
 * <p>Blowfish takes 64 bit plaintext blocks as input and creates as output 
 * 64-bit ciphertext. The key size for Blowfish can range from 32 bits all the 
 * way up to 448 bits which implies flexibility in its security strength. The 
 * input block is split in half L0 and R0 each consisting of 32 bits. Blowfish 
 * can essentially be described by the following algorithm:<br>
 * <pre>
 * j = 1
 * loop from j to 16
 *   Rj = Lj-1 XoR Pj
 *   Lj = F(Rj) XoR Rj-1
 * end loop
 * L17 = R16 XoR P18
 * R17 = L16 XoR P17
 * </pre>
 * Where P is the sub-keys and F is the complex function. L17 and R17 contain 
 * the ciphertext. Notice that there are 16 iterations hence 16 rounds of 
 * XoR'ing and operations of F.</p>
 * 
 * <h3>Blowfish Decryption</h3>
 * <p>Decryption for Blowfish is relatively straight forward. Ironically, 
 * decryption works in the same algorithmic direction as encryption beginning 
 * with the ciphertext as input. However, as expected, the sub-keys are used in 
 * reverse order. So the decryption Blowfish algorithm is as follows:<br>
 * <pre>
 * j = 1
 * loop from j to 16
 *   Rj = Lj-1 XoR P19-j
 *   Lj = F(Rj)XoR Rj-1
 * end loop
 * L17 = R16 XoR P1
 * R17 = L16 XoR P2
 * </pre>
 * 
 * <h3>Sub-Keys, SBoxes and P-Array</h3>
 * <p>Generating the sub-keys and the SBoxes can be described in 3 steps below. 
 * Considering that the key can be 32 to 448 bits there can exist 1 to 14 
 * 32-bit words. This key is then used to make 4 SBoxes and 18 32 bit sub-keys. 
 * The SBoxes have an 8 x 32 structure which totals 256 32 bit elements. The 
 * P-Array stores the sub-keys.</p>
 * 
 * <p><strong>Step 1</strong>: The P-Array becomes initialized in an orderly 
 * fashion by using bits from the constant pi. For instance, P1 is assigned the 
 * leftmost 32 bits of pi and so forth. Next the 4 SBoxes becomes 
 * initialized.</p>
 * <p><strong>Step 2</strong>: An XoR is conducted with the array elements of 
 * the key and the sub-keys P-Array elements and then reassigned into the 
 * P-Array elements. For instance, Pi = Pi XoR Kj...</p>
 * <p><strong>Step 3</strong>: There should now be a 64 bit block (of all zeros 
 * for the first case). Take this block and encrypt it using the Blowfish 
 * process. Pi and Pi+1 will then be replaced with this result and then 
 * increment i. Continue this step until all P-Array elements have been 
 * replaced and then in order all 4 SBoxes have been replaced likewise.</p>
 * 
 * <h3>Benefits of Blowfish</h3>
 * <p>Blowfish has been known to be very fast and compact only requiring 5K of 
 * RAM. Variability exists in key length and Blowfish is relatively simple to 
 * implement. Blowfish provides a little stronger cryptographic process by 
 * performing operations on both halves of its input word per round which is 
 * different than the classical Feistal process. Finally, Blowfish provides a 
 * very strong avalanche affect in that every left-side input bit affects every 
 * right-side input bit per round. </p>
 * 
 * <h3>Status of Blowfish</h3>
 * <p>Blowfish is classified as public domain; as such it has been analyzed 
 * extensively and gone through years of peer review. At no point since it's 
 * initial release in 1993 has the Blowfish code ever been cracked. This is 
 * significant when you consider that the source code to the algorithm is 
 * freely available.</p>
 * 
 * <p>The relative strength of the encryption algorithm is based on key length. 
 * Bruce Schneier, creator of the Blowfish encryption algorithm, has calculated 
 * that according to what we know of quantum mechanics today, that the entire 
 * energy output of the sun is insufficient to break a 197-bit key.</p>
 * 
 * <p>Here is a more generalized example:<br/>The most common key lengths used 
 * by today's web browsers are "40-bit" and "128-bit." As a comparison, a 
 * 40-bit key can be "cracked" within a few hours by an average personal 
 * computer. However, a 128-bit key would take one BILLION powerful computers, 
 * each capable of trying one BILLION keys per second. In other words, it would 
 * take MILLIONS of years to try every possible combination of bits in a 
 * 128-bit key.</p>
 * 
 * <p>In the preceding example, the 128-bit encryption is not just three times 
 * stronger than 40-bit encryption - it is 
 * 309,485,009,821,345,068,724,781,056 times stronger. Performing this same 
 * analysis on a 448-bit encryption key yields an encryption strength that is 
 * 2.1X10^96 times stronger than a 128-bit key.</p>
 * 
 * <p>Ported to Java from the C-code reference implementation.</p>
 * 
 * <p>References:<ol>
 * <li>Applied Cryptography --2nd Edition. Bruce Schneier; John Wiley & Sons
 * 1996, 336-339.</ul></p>
 * <li><a href="http://www.counterpane.com/blowfish.html">The Blowfish 
 * Encryption Algorithm.</a></li>
 * </ol>
 */
class MessageCipher
{

  /**
   * Cipher initialization data.
   */
  private static final int[] Pi = { 0x243F6A88, 0x85A308D3, 0x13198A2E, 0x03707344, 0xA4093822, 0x299F31D0, 0x082EFA98, 0xEC4E6C89, 0x452821E6, 0x38D01377, 0xBE5466CF, 0x34E90C6C, 0xC0AC29B7, 0xC97C50DD, 0x3F84D5B5, 0xB5470917, 0x9216D5D9, 0x8979FB1B };

  private static final int[] S0 = { 0xD1310BA6, 0x98DFB5AC, 0x2FFD72DB, 0xD01ADFB7, 0xB8E1AFED, 0x6A267E96, 0xBA7C9045, 0xF12C7F99, 0x24A19947, 0xB3916CF7, 0x0801F2E2, 0x858EFC16, 0x636920D8, 0x71574E69, 0xA458FEA3, 0xF4933D7E, 0x0D95748F, 0x728EB658, 0x718BCD58, 0x82154AEE, 0x7B54A41D, 0xC25A59B5, 0x9C30D539, 0x2AF26013, 0xC5D1B023, 0x286085F0, 0xCA417918, 0xB8DB38EF, 0x8E79DCB0, 0x603A180E, 0x6C9E0E8B, 0xB01E8A3E, 0xD71577C1, 0xBD314B27, 0x78AF2FDA, 0x55605C60, 0xE65525F3, 0xAA55AB94, 0x57489862, 0x63E81440, 0x55CA396A, 0x2AAB10B6, 0xB4CC5C34, 0x1141E8CE, 0xA15486AF, 0x7C72E993, 0xB3EE1411, 0x636FBC2A, 0x2BA9C55D, 0x741831F6, 0xCE5C3E16, 0x9B87931E, 0xAFD6BA33, 0x6C24CF5C, 0x7A325381, 0x28958677, 0x3B8F4898, 0x6B4BB9AF, 0xC4BFE81B, 0x66282193, 0x61D809CC, 0xFB21A991, 0x487CAC60, 0x5DEC8032, 0xEF845D5D, 0xE98575B1, 0xDC262302, 0xEB651B88, 0x23893E81, 0xD396ACC5, 0x0F6D6FF3, 0x83F44239, 0x2E0B4482, 0xA4842004, 0x69C8F04A, 0x9E1F9B5E, 0x21C66842, 0xF6E96C9A, 0x670C9C61, 0xABD388F0, 0x6A51A0D2, 0xD8542F68,
      0x960FA728, 0xAB5133A3, 0x6EEF0B6C, 0x137A3BE4, 0xBA3BF050, 0x7EFB2A98, 0xA1F1651D, 0x39AF0176, 0x66CA593E, 0x82430E88, 0x8CEE8619, 0x456F9FB4, 0x7D84A5C3, 0x3B8B5EBE, 0xE06F75D8, 0x85C12073, 0x401A449F, 0x56C16AA6, 0x4ED3AA62, 0x363F7706, 0x1BFEDF72, 0x429B023D, 0x37D0D724, 0xD00A1248, 0xDB0FEAD3, 0x49F1C09B, 0x075372C9, 0x80991B7B, 0x25D479D8, 0xF6E8DEF7, 0xE3FE501A, 0xB6794C3B, 0x976CE0BD, 0x04C006BA, 0xC1A94FB6, 0x409F60C4, 0x5E5C9EC2, 0x196A2463, 0x68FB6FAF, 0x3E6C53B5, 0x1339B2EB, 0x3B52EC6F, 0x6DFC511F, 0x9B30952C, 0xCC814544, 0xAF5EBD09, 0xBEE3D004, 0xDE334AFD, 0x660F2807, 0x192E4BB3, 0xC0CBA857, 0x45C8740F, 0xD20B5F39, 0xB9D3FBDB, 0x5579C0BD, 0x1A60320A, 0xD6A100C6, 0x402C7279, 0x679F25FE, 0xFB1FA3CC, 0x8EA5E9F8, 0xDB3222F8, 0x3C7516DF, 0xFD616B15, 0x2F501EC8, 0xAD0552AB, 0x323DB5FA, 0xFD238760, 0x53317B48, 0x3E00DF82, 0x9E5C57BB, 0xCA6F8CA0, 0x1A87562E, 0xDF1769DB, 0xD542A8F6, 0x287EFFC3, 0xAC6732C6, 0x8C4F5573, 0x695B27B0, 0xBBCA58C8, 0xE1FFA35D, 0xB8F011A0, 0x10FA3D98, 0xFD2183B8,
      0x4AFCB56C, 0x2DD1D35B, 0x9A53E479, 0xB6F84565, 0xD28E49BC, 0x4BFB9790, 0xE1DDF2DA, 0xA4CB7E33, 0x62FB1341, 0xCEE4C6E8, 0xEF20CADA, 0x36774C01, 0xD07E9EFE, 0x2BF11FB4, 0x95DBDA4D, 0xAE909198, 0xEAAD8E71, 0x6B93D5A0, 0xD08ED1D0, 0xAFC725E0, 0x8E3C5B2F, 0x8E7594B7, 0x8FF6E2FB, 0xF2122B64, 0x8888B812, 0x900DF01C, 0x4FAD5EA0, 0x688FC31C, 0xD1CFF191, 0xB3A8C1AD, 0x2F2F2218, 0xBE0E1777, 0xEA752DFE, 0x8B021FA1, 0xE5A0CC0F, 0xB56F74E8, 0x18ACF3D6, 0xCE89E299, 0xB4A84FE0, 0xFD13E0B7, 0x7CC43B81, 0xD2ADA8D9, 0x165FA266, 0x80957705, 0x93CC7314, 0x211A1477, 0xE6AD2065, 0x77B5FA86, 0xC75442F5, 0xFB9D35CF, 0xEBCDAF0C, 0x7B3E89A0, 0xD6411BD3, 0xAE1E7E49, 0x00250E2D, 0x2071B35E, 0x226800BB, 0x57B8E0AF, 0x2464369B, 0xF009B91E, 0x5563911D, 0x59DFA6AA, 0x78C14389, 0xD95A537F, 0x207D5BA2, 0x02E5B9C5, 0x83260376, 0x6295CFA9, 0x11C81968, 0x4E734A41, 0xB3472DCA, 0x7B14A94A, 0x1B510052, 0x9A532915, 0xD60F573F, 0xBC9BC6E4, 0x2B60A476, 0x81E67400, 0x08BA6FB5, 0x571BE91F, 0xF296EC6B, 0x2A0DD915, 0xB6636521, 0xE7B9F9B6,
      0xFF34052E, 0xC5855664, 0x53B02D5D, 0xA99F8FA1, 0x08BA4799, 0x6E85076A };

  private static final int[] S1 = { 0x4B7A70E9, 0xB5B32944, 0xDB75092E, 0xC4192623, 0xAD6EA6B0, 0x49A7DF7D, 0x9CEE60B8, 0x8FEDB266, 0xECAA8C71, 0x699A17FF, 0x5664526C, 0xC2B19EE1, 0x193602A5, 0x75094C29, 0xA0591340, 0xE4183A3E, 0x3F54989A, 0x5B429D65, 0x6B8FE4D6, 0x99F73FD6, 0xA1D29C07, 0xEFE830F5, 0x4D2D38E6, 0xF0255DC1, 0x4CDD2086, 0x8470EB26, 0x6382E9C6, 0x021ECC5E, 0x09686B3F, 0x3EBAEFC9, 0x3C971814, 0x6B6A70A1, 0x687F3584, 0x52A0E286, 0xB79C5305, 0xAA500737, 0x3E07841C, 0x7FDEAE5C, 0x8E7D44EC, 0x5716F2B8, 0xB03ADA37, 0xF0500C0D, 0xF01C1F04, 0x0200B3FF, 0xAE0CF51A, 0x3CB574B2, 0x25837A58, 0xDC0921BD, 0xD19113F9, 0x7CA92FF6, 0x94324773, 0x22F54701, 0x3AE5E581, 0x37C2DADC, 0xC8B57634, 0x9AF3DDA7, 0xA9446146, 0x0FD0030E, 0xECC8C73E, 0xA4751E41, 0xE238CD99, 0x3BEA0E2F, 0x3280BBA1, 0x183EB331, 0x4E548B38, 0x4F6DB908, 0x6F420D03, 0xF60A04BF, 0x2CB81290, 0x24977C79, 0x5679B072, 0xBCAF89AF, 0xDE9A771F, 0xD9930810, 0xB38BAE12, 0xDCCF3F2E, 0x5512721F, 0x2E6B7124, 0x501ADDE6, 0x9F84CD87, 0x7A584718, 0x7408DA17,
      0xBC9F9ABC, 0xE94B7D8C, 0xEC7AEC3A, 0xDB851DFA, 0x63094366, 0xC464C3D2, 0xEF1C1847, 0x3215D908, 0xDD433B37, 0x24C2BA16, 0x12A14D43, 0x2A65C451, 0x50940002, 0x133AE4DD, 0x71DFF89E, 0x10314E55, 0x81AC77D6, 0x5F11199B, 0x043556F1, 0xD7A3C76B, 0x3C11183B, 0x5924A509, 0xF28FE6ED, 0x97F1FBFA, 0x9EBABF2C, 0x1E153C6E, 0x86E34570, 0xEAE96FB1, 0x860E5E0A, 0x5A3E2AB3, 0x771FE71C, 0x4E3D06FA, 0x2965DCB9, 0x99E71D0F, 0x803E89D6, 0x5266C825, 0x2E4CC978, 0x9C10B36A, 0xC6150EBA, 0x94E2EA78, 0xA5FC3C53, 0x1E0A2DF4, 0xF2F74EA7, 0x361D2B3D, 0x1939260F, 0x19C27960, 0x5223A708, 0xF71312B6, 0xEBADFE6E, 0xEAC31F66, 0xE3BC4595, 0xA67BC883, 0xB17F37D1, 0x018CFF28, 0xC332DDEF, 0xBE6C5AA5, 0x65582185, 0x68AB9802, 0xEECEA50F, 0xDB2F953B, 0x2AEF7DAD, 0x5B6E2F84, 0x1521B628, 0x29076170, 0xECDD4775, 0x619F1510, 0x13CCA830, 0xEB61BD96, 0x0334FE1E, 0xAA0363CF, 0xB5735C90, 0x4C70A239, 0xD59E9E0B, 0xCBAADE14, 0xEECC86BC, 0x60622CA7, 0x9CAB5CAB, 0xB2F3846E, 0x648B1EAF, 0x19BDF0CA, 0xA02369B9, 0x655ABB50, 0x40685A32, 0x3C2AB4B3,
      0x319EE9D5, 0xC021B8F7, 0x9B540B19, 0x875FA099, 0x95F7997E, 0x623D7DA8, 0xF837889A, 0x97E32D77, 0x11ED935F, 0x16681281, 0x0E358829, 0xC7E61FD6, 0x96DEDFA1, 0x7858BA99, 0x57F584A5, 0x1B227263, 0x9B83C3FF, 0x1AC24696, 0xCDB30AEB, 0x532E3054, 0x8FD948E4, 0x6DBC3128, 0x58EBF2EF, 0x34C6FFEA, 0xFE28ED61, 0xEE7C3C73, 0x5D4A14D9, 0xE864B7E3, 0x42105D14, 0x203E13E0, 0x45EEE2B6, 0xA3AAABEA, 0xDB6C4F15, 0xFACB4FD0, 0xC742F442, 0xEF6ABBB5, 0x654F3B1D, 0x41CD2105, 0xD81E799E, 0x86854DC7, 0xE44B476A, 0x3D816250, 0xCF62A1F2, 0x5B8D2646, 0xFC8883A0, 0xC1C7B6A3, 0x7F1524C3, 0x69CB7492, 0x47848A0B, 0x5692B285, 0x095BBF00, 0xAD19489D, 0x1462B174, 0x23820E00, 0x58428D2A, 0x0C55F5EA, 0x1DADF43E, 0x233F7061, 0x3372F092, 0x8D937E41, 0xD65FECF1, 0x6C223BDB, 0x7CDE3759, 0xCBEE7460, 0x4085F2A7, 0xCE77326E, 0xA6078084, 0x19F8509E, 0xE8EFD855, 0x61D99735, 0xA969A7AA, 0xC50C06C2, 0x5A04ABFC, 0x800BCADC, 0x9E447A2E, 0xC3453484, 0xFDD56705, 0x0E1E9EC9, 0xDB73DBD3, 0x105588CD, 0x675FDA79, 0xE3674340, 0xC5C43465, 0x713E38D8,
      0x3D28F89E, 0xF16DFF20, 0x153E21E7, 0x8FB03D4A, 0xE6E39F2B, 0xDB83ADF7 };

  private static final int[] S2 = { 0xE93D5A68, 0x948140F7, 0xF64C261C, 0x94692934, 0x411520F7, 0x7602D4F7, 0xBCF46B2E, 0xD4A20068, 0xD4082471, 0x3320F46A, 0x43B7D4B7, 0x500061AF, 0x1E39F62E, 0x97244546, 0x14214F74, 0xBF8B8840, 0x4D95FC1D, 0x96B591AF, 0x70F4DDD3, 0x66A02F45, 0xBFBC09EC, 0x03BD9785, 0x7FAC6DD0, 0x31CB8504, 0x96EB27B3, 0x55FD3941, 0xDA2547E6, 0xABCA0A9A, 0x28507825, 0x530429F4, 0x0A2C86DA, 0xE9B66DFB, 0x68DC1462, 0xD7486900, 0x680EC0A4, 0x27A18DEE, 0x4F3FFEA2, 0xE887AD8C, 0xB58CE006, 0x7AF4D6B6, 0xAACE1E7C, 0xD3375FEC, 0xCE78A399, 0x406B2A42, 0x20FE9E35, 0xD9F385B9, 0xEE39D7AB, 0x3B124E8B, 0x1DC9FAF7, 0x4B6D1856, 0x26A36631, 0xEAE397B2, 0x3A6EFA74, 0xDD5B4332, 0x6841E7F7, 0xCA7820FB, 0xFB0AF54E, 0xD8FEB397, 0x454056AC, 0xBA489527, 0x55533A3A, 0x20838D87, 0xFE6BA9B7, 0xD096954B, 0x55A867BC, 0xA1159A58, 0xCCA92963, 0x99E1DB33, 0xA62A4A56, 0x3F3125F9, 0x5EF47E1C, 0x9029317C, 0xFDF8E802, 0x04272F70, 0x80BB155C, 0x05282CE3, 0x95C11548, 0xE4C66D22, 0x48C1133F, 0xC70F86DC, 0x07F9C9EE, 0x41041F0F,
      0x404779A4, 0x5D886E17, 0x325F51EB, 0xD59BC0D1, 0xF2BCC18F, 0x41113564, 0x257B7834, 0x602A9C60, 0xDFF8E8A3, 0x1F636C1B, 0x0E12B4C2, 0x02E1329E, 0xAF664FD1, 0xCAD18115, 0x6B2395E0, 0x333E92E1, 0x3B240B62, 0xEEBEB922, 0x85B2A20E, 0xE6BA0D99, 0xDE720C8C, 0x2DA2F728, 0xD0127845, 0x95B794FD, 0x647D0862, 0xE7CCF5F0, 0x5449A36F, 0x877D48FA, 0xC39DFD27, 0xF33E8D1E, 0x0A476341, 0x992EFF74, 0x3A6F6EAB, 0xF4F8FD37, 0xA812DC60, 0xA1EBDDF8, 0x991BE14C, 0xDB6E6B0D, 0xC67B5510, 0x6D672C37, 0x2765D43B, 0xDCD0E804, 0xF1290DC7, 0xCC00FFA3, 0xB5390F92, 0x690FED0B, 0x667B9FFB, 0xCEDB7D9C, 0xA091CF0B, 0xD9155EA3, 0xBB132F88, 0x515BAD24, 0x7B9479BF, 0x763BD6EB, 0x37392EB3, 0xCC115979, 0x8026E297, 0xF42E312D, 0x6842ADA7, 0xC66A2B3B, 0x12754CCC, 0x782EF11C, 0x6A124237, 0xB79251E7, 0x06A1BBE6, 0x4BFB6350, 0x1A6B1018, 0x11CAEDFA, 0x3D25BDD8, 0xE2E1C3C9, 0x44421659, 0x0A121386, 0xD90CEC6E, 0xD5ABEA2A, 0x64AF674E, 0xDA86A85F, 0xBEBFE988, 0x64E4C3FE, 0x9DBC8057, 0xF0F7C086, 0x60787BF8, 0x6003604D, 0xD1FD8346, 0xF6381FB0,
      0x7745AE04, 0xD736FCCC, 0x83426B33, 0xF01EAB71, 0xB0804187, 0x3C005E5F, 0x77A057BE, 0xBDE8AE24, 0x55464299, 0xBF582E61, 0x4E58F48F, 0xF2DDFDA2, 0xF474EF38, 0x8789BDC2, 0x5366F9C3, 0xC8B38E74, 0xB475F255, 0x46FCD9B9, 0x7AEB2661, 0x8B1DDF84, 0x846A0E79, 0x915F95E2, 0x466E598E, 0x20B45770, 0x8CD55591, 0xC902DE4C, 0xB90BACE1, 0xBB8205D0, 0x11A86248, 0x7574A99E, 0xB77F19B6, 0xE0A9DC09, 0x662D09A1, 0xC4324633, 0xE85A1F02, 0x09F0BE8C, 0x4A99A025, 0x1D6EFE10, 0x1AB93D1D, 0x0BA5A4DF, 0xA186F20F, 0x2868F169, 0xDCB7DA83, 0x573906FE, 0xA1E2CE9B, 0x4FCD7F52, 0x50115E01, 0xA70683FA, 0xA002B5C4, 0x0DE6D027, 0x9AF88C27, 0x773F8641, 0xC3604C06, 0x61A806B5, 0xF0177A28, 0xC0F586E0, 0x006058AA, 0x30DC7D62, 0x11E69ED7, 0x2338EA63, 0x53C2DD94, 0xC2C21634, 0xBBCBEE56, 0x90BCB6DE, 0xEBFC7DA1, 0xCE591D76, 0x6F05E409, 0x4B7C0188, 0x39720A3D, 0x7C927C24, 0x86E3725F, 0x724D9DB9, 0x1AC15BB4, 0xD39EB8FC, 0xED545578, 0x08FCA5B5, 0xD83D7CD3, 0x4DAD0FC4, 0x1E50EF5E, 0xB161E6F8, 0xA28514D9, 0x6C51133C, 0x6FD5C7E7, 0x56E14EC4,
      0x362ABFCE, 0xDDC6C837, 0xD79A3234, 0x92638212, 0x670EFA8E, 0x406000E0 };

  private static final int[] S3 = { 0x3A39CE37, 0xD3FAF5CF, 0xABC27737, 0x5AC52D1B, 0x5CB0679E, 0x4FA33742, 0xD3822740, 0x99BC9BBE, 0xD5118E9D, 0xBF0F7315, 0xD62D1C7E, 0xC700C47B, 0xB78C1B6B, 0x21A19045, 0xB26EB1BE, 0x6A366EB4, 0x5748AB2F, 0xBC946E79, 0xC6A376D2, 0x6549C2C8, 0x530FF8EE, 0x468DDE7D, 0xD5730A1D, 0x4CD04DC6, 0x2939BBDB, 0xA9BA4650, 0xAC9526E8, 0xBE5EE304, 0xA1FAD5F0, 0x6A2D519A, 0x63EF8CE2, 0x9A86EE22, 0xC089C2B8, 0x43242EF6, 0xA51E03AA, 0x9CF2D0A4, 0x83C061BA, 0x9BE96A4D, 0x8FE51550, 0xBA645BD6, 0x2826A2F9, 0xA73A3AE1, 0x4BA99586, 0xEF5562E9, 0xC72FEFD3, 0xF752F7DA, 0x3F046F69, 0x77FA0A59, 0x80E4A915, 0x87B08601, 0x9B09E6AD, 0x3B3EE593, 0xE990FD5A, 0x9E34D797, 0x2CF0B7D9, 0x022B8B51, 0x96D5AC3A, 0x017DA67D, 0xD1CF3ED6, 0x7C7D2D28, 0x1F9F25CF, 0xADF2B89B, 0x5AD6B472, 0x5A88F54C, 0xE029AC71, 0xE019A5E6, 0x47B0ACFD, 0xED93FA9B, 0xE8D3C48D, 0x283B57CC, 0xF8D56629, 0x79132E28, 0x785F0191, 0xED756055, 0xF7960E44, 0xE3D35E8C, 0x15056DD4, 0x88F46DBA, 0x03A16125, 0x0564F0BD, 0xC3EB9E15, 0x3C9057A2,
      0x97271AEC, 0xA93A072A, 0x1B3F6D9B, 0x1E6321F5, 0xF59C66FB, 0x26DCF319, 0x7533D928, 0xB155FDF5, 0x03563482, 0x8ABA3CBB, 0x28517711, 0xC20AD9F8, 0xABCC5167, 0xCCAD925F, 0x4DE81751, 0x3830DC8E, 0x379D5862, 0x9320F991, 0xEA7A90C2, 0xFB3E7BCE, 0x5121CE64, 0x774FBE32, 0xA8B6E37E, 0xC3293D46, 0x48DE5369, 0x6413E680, 0xA2AE0810, 0xDD6DB224, 0x69852DFD, 0x09072166, 0xB39A460A, 0x6445C0DD, 0x586CDECF, 0x1C20C8AE, 0x5BBEF7DD, 0x1B588D40, 0xCCD2017F, 0x6BB4E3BB, 0xDDA26A7E, 0x3A59FF45, 0x3E350A44, 0xBCB4CDD5, 0x72EACEA8, 0xFA6484BB, 0x8D6612AE, 0xBF3C6F47, 0xD29BE463, 0x542F5D9E, 0xAEC2771B, 0xF64E6370, 0x740E0D8D, 0xE75B1357, 0xF8721671, 0xAF537D5D, 0x4040CB08, 0x4EB4E2CC, 0x34D2466A, 0x0115AF84, 0xE1B00428, 0x95983A1D, 0x06B89FB4, 0xCE6EA048, 0x6F3F3B82, 0x3520AB82, 0x011A1D4B, 0x277227F8, 0x611560B1, 0xE7933FDC, 0xBB3A792B, 0x344525BD, 0xA08839E1, 0x51CE794B, 0x2F32C9B7, 0xA01FBAC9, 0xE01CC87E, 0xBCC7D1F6, 0xCF0111C3, 0xA1E8AAC7, 0x1A908749, 0xD44FBD9A, 0xD0DADECB, 0xD50ADA38, 0x0339C32A, 0xC6913667,
      0x8DF9317C, 0xE0B12B4F, 0xF79E59B7, 0x43F5BB3A, 0xF2D519FF, 0x27D9459C, 0xBF97222C, 0x15E6FC2A, 0x0F91FC71, 0x9B941525, 0xFAE59361, 0xCEB69CEB, 0xC2A86459, 0x12BAA8D1, 0xB6C1075E, 0xE3056A0C, 0x10D25065, 0xCB03A442, 0xE0EC6E0E, 0x1698DB3B, 0x4C98A0BE, 0x3278E964, 0x9F1F9532, 0xE0D392DF, 0xD3A0342B, 0x8971F21E, 0x1B0A7441, 0x4BA3348C, 0xC5BE7120, 0xC37632D8, 0xDF359F8D, 0x9B992F2E, 0xE60B6F47, 0x0FE3F11D, 0xE54CDA54, 0x1EDAD891, 0xCE6279CF, 0xCD3E7E6F, 0x1618B166, 0xFD2C1D05, 0x848FD2C5, 0xF6FB2299, 0xF523F357, 0xA6327623, 0x93A83531, 0x56CCCD02, 0xACF08162, 0x5A75EBB5, 0x6E163697, 0x88D273CC, 0xDE966292, 0x81B949D0, 0x4C50901B, 0x71C65614, 0xE6C6C7BD, 0x327A140A, 0x45E1D006, 0xC3F27B9A, 0xC9AA53FD, 0x62A80F00, 0xBB25BFE2, 0x35BDD2F6, 0x71126905, 0xB2040222, 0xB6CBCF7C, 0xCD769C2B, 0x53113EC0, 0x1640E3D3, 0x38ABBD60, 0x2547ADF0, 0xBA38209C, 0xF746CE76, 0x77AFA1C5, 0x20756060, 0x85CBFE4E, 0x8AE88DD8, 0x7AAAF9B0, 0x4CF9AA7E, 0x1948C25C, 0x02FB8A8C, 0x01C36AE4, 0xD6EBE1F9, 0x90D4F869, 0xA65CDEA0,
      0x3F09252D, 0xC208E69F, 0xB74E6132, 0xCE77E25B, 0x578FDFE3, 0x3AC372E6 };

  private static final int ROUNDS = 16;

  // Blowfish block size in bytes
  private static final int BLOCK_SIZE = 8;

  //given in bytes from a value in bits
  private static final int MAX_USER_KEY_LENGTH = 448 / 8;

  // This Blowfish object session key and s-boxes data placeholders.
  private final int[] P = new int[18];
  private final int[] sKey = new int[4 * 256];




  /**
   * Constructs a MessageCipher cipher object.</p>
   */
  public MessageCipher()
  {
  }




  /**
   * Initializes this cipher, using the specified key.
   *
   * @param key the key to use for encryption.
   * @throws IllegalArgumentException when one of the following occurs:<ul>
   *    <li>The Key object is null;
   *    <li>The encoded byte array form of the key is a zero length one.</ul>
   */
  public void init( final byte[] key ) throws IllegalArgumentException
  {
    makeKey( key );
  }




  /**
   *
   */
  public byte[] decrypt( final byte[] data )
  {
    int inOff = 0;
    final int blockCount = data.length / MessageCipher.BLOCK_SIZE;
    final byte[] retval = new byte[blockCount * MessageCipher.BLOCK_SIZE];
    for( int i = 0; i < blockCount; i++ )
    {
      blowfishDecrypt( data, inOff, retval, i * MessageCipher.BLOCK_SIZE );
      inOff += MessageCipher.BLOCK_SIZE;
    }

    return retval;
  }




  /**
   *
   */
  public byte[] encrypt( final byte[] bytes )
  {
    int inOff = 0;
    final int blockCount = bytes.length / MessageCipher.BLOCK_SIZE;
    final byte[] outs = new byte[blockCount * MessageCipher.BLOCK_SIZE];
    for( int i = 0; i < blockCount; i++ )
    {
      blowfishEncrypt( bytes, inOff, outs, i * MessageCipher.BLOCK_SIZE );
      inOff += MessageCipher.BLOCK_SIZE;
    }
    return outs;
  }




  /**
   * The normal entry to the encryption process. 
   * 
   * <p>It is guaranteed to be called with enough bytes in the input to carry 
   * on an encryption of one full block.</p>
   * 
   * <p>The code of the Blowfish encryption engine, found here, is also 
   * replicated in the makeSessionKey method found later. The reasons for this 
   * duplication is performance. This method, outputs the result in a byte 
   * array form, suitable for the user data encryption operations, while 
   * makeSessionKey outputs its result as an int array suitable for, and used 
   * during, the expansion of the user-key into a Blowfish session key.</p>
   *
   * @param in input byte array of plain text.
   * @param off index in in of where to start considering data.
   * @param out will contain the cipher-text block.
   * @param outOff index in out where cipher-text starts.
   */
  private void blowfishEncrypt( final byte[] in, final int off, final byte[] out, final int outOff )
  {
    int L = ( ( in[off] & 0xFF ) << 24 ) | ( ( in[off + 1] & 0xFF ) << 16 ) | ( ( in[off + 2] & 0xFF ) << 8 ) | ( in[off + 3] & 0xFF ), R = ( ( in[off + 4] & 0xFF ) << 24 ) | ( ( in[off + 5] & 0xFF ) << 16 ) | ( ( in[off + 6] & 0xFF ) << 8 ) | ( in[off + 7] & 0xFF ), a, b, c, d;

    L ^= P[0];
    for( int i = 0; i < MessageCipher.ROUNDS; )
    {
      a = ( L >>> 24 ) & 0xFF;
      b = 0x100 | ( ( L >>> 16 ) & 0xFF ); // 256 +
      c = 0x200 | ( ( L >>> 8 ) & 0xFF ); // 512 +
      d = 0x300 | L & 0xFF; // 768 +
      R ^= ( ( ( sKey[a] + sKey[b] ) ^ sKey[c] ) + sKey[d] ) ^ P[++i];

      a = ( R >>> 24 ) & 0xFF;
      b = 0x100 | ( ( R >>> 16 ) & 0xFF ); // 256 +
      c = 0x200 | ( ( R >>> 8 ) & 0xFF ); // 512 +
      d = 0x300 | R & 0xFF; // 768 +
      L ^= ( ( ( sKey[a] + sKey[b] ) ^ sKey[c] ) + sKey[d] ) ^ P[++i];
    }
    R ^= P[MessageCipher.ROUNDS + 1];

    out[outOff] = (byte)( ( R >>> 24 ) & 0xFF );
    out[outOff + 1] = (byte)( ( R >>> 16 ) & 0xFF );
    out[outOff + 2] = (byte)( ( R >>> 8 ) & 0xFF );
    out[outOff + 3] = (byte)( R & 0xFF );
    out[outOff + 4] = (byte)( ( L >>> 24 ) & 0xFF );
    out[outOff + 5] = (byte)( ( L >>> 16 ) & 0xFF );
    out[outOff + 6] = (byte)( ( L >>> 8 ) & 0xFF );
    out[outOff + 7] = (byte)( L & 0xFF );
  }




  /**
   * The normal entry to the decryption process. 
   * 
   * <p>It is guaranteed to be called with enough bytes in the input to carry 
   * on a decryption of one full block.</p>
   * 
   * <p>Because the Blowfish cipher engine is designed to handle two 32-bit 
   * blocks, this method's purpose is to transform on entry and exit the data 
   * to/from 32-bit blocks; ie. Java.int.</p>
   * 
   * <p>The input becomes two 32-bit blocks as Left and Right halves onto which 
   * the Blowfish cipher function is applied ROUNDS times in reverse order to 
   * that of the encryption.</p>
   *
   * @param in input byte array of cipher text.
   * @param off index in in of where to start considering data.
   * @param out will contain the plain-text block.
   * @param outOff index in out where plain-text starts.
   */
  private void blowfishDecrypt( final byte[] in, final int off, final byte[] out, final int outOff )
  {
    int L = ( ( in[off] & 0xFF ) << 24 ) | ( ( in[off + 1] & 0xFF ) << 16 ) | ( ( in[off + 2] & 0xFF ) << 8 ) | ( in[off + 3] & 0xFF ), R = ( ( in[off + 4] & 0xFF ) << 24 ) | ( ( in[off + 5] & 0xFF ) << 16 ) | ( ( in[off + 6] & 0xFF ) << 8 ) | ( in[off + 7] & 0xFF );

    int a, b, c, d;

    L ^= P[MessageCipher.ROUNDS + 1];
    for( int i = MessageCipher.ROUNDS; i > 0; )
    {
      a = ( L >>> 24 ) & 0xFF;
      b = 0x100 | ( ( L >>> 16 ) & 0xFF ); // 256 +
      c = 0x200 | ( ( L >>> 8 ) & 0xFF ); // 512 +
      d = 0x300 | L & 0xFF; // 768 +
      R ^= ( ( ( sKey[a] + sKey[b] ) ^ sKey[c] ) + sKey[d] ) ^ P[i--];

      a = ( R >>> 24 ) & 0xFF;
      b = 0x100 | ( ( R >>> 16 ) & 0xFF ); // 256 +
      c = 0x200 | ( ( R >>> 8 ) & 0xFF ); // 512 +
      d = 0x300 | R & 0xFF; // 768 +
      L ^= ( ( ( sKey[a] + sKey[b] ) ^ sKey[c] ) + sKey[d] ) ^ P[i--];
    }
    R ^= P[0];

    out[outOff] = (byte)( ( R >>> 24 ) & 0xFF );
    out[outOff + 1] = (byte)( ( R >>> 16 ) & 0xFF );
    out[outOff + 2] = (byte)( ( R >>> 8 ) & 0xFF );
    out[outOff + 3] = (byte)( R & 0xFF );
    out[outOff + 4] = (byte)( ( L >>> 24 ) & 0xFF );
    out[outOff + 5] = (byte)( ( L >>> 16 ) & 0xFF );
    out[outOff + 6] = (byte)( ( L >>> 8 ) & 0xFF );
    out[outOff + 7] = (byte)( L & 0xFF );
  }




  /**
   *This method is only called by the makeKey method to generate a session 
   * key from user data. It outputs the result to an int array.
   *
   * @see #blowfishEncrypt
   * 
   * @param L Left half (32-bit) of the plain text block,
   * @param R Right half (32-bit) of the plain text block.
   * @param out the int array where the result will be saved.
   * @param outOff where the data starts in the byte array.
   */
  private final void makeSessionKey( int L, int R, final int[] out, final int outOff )
  {
    int a, b, c, d;

    L ^= P[0];
    for( int i = 0; i < MessageCipher.ROUNDS; )
    {
      a = ( L >>> 24 ) & 0xFF;
      b = 0x100 | ( ( L >>> 16 ) & 0xFF ); // 256 +
      c = 0x200 | ( ( L >>> 8 ) & 0xFF ); // 512 +
      d = 0x300 | L & 0xFF; // 768 +
      R ^= ( ( ( sKey[a] + sKey[b] ) ^ sKey[c] ) + sKey[d] ) ^ P[++i];

      a = ( R >>> 24 ) & 0xFF;
      b = 0x100 | ( ( R >>> 16 ) & 0xFF ); // 256 +
      c = 0x200 | ( ( R >>> 8 ) & 0xFF ); // 512 +
      d = 0x300 | R & 0xFF; // 768 +
      L ^= ( ( ( sKey[a] + sKey[b] ) ^ sKey[c] ) + sKey[d] ) ^ P[++i];
    }
    R ^= P[MessageCipher.ROUNDS + 1];

    out[outOff] = R;
    out[outOff + 1] = L;
  }




  /**
   * Expands a userKey to a working Blowfish session key(P) and generates this 
   * session s-boxes data (sKey).
   * 
   * <p>The key bytes are fist extracted from the user-key and then used, 
   * repetitively if need be, to build the contents of this session key and 
   * S-boxes values.</p>
   * 
   * <p>The method's only exceptions are when the user-key's contents is a null 
   * Java object or a byte array of zero length. Otherwise the key data -up to 
   * 56 bytes- are used repetitively.</p>
   *
   * @param key the user-key object to use for this session.
   * 
   * @throws KeyException when one of the following occurs:<ul>
   *    <li>The Key object is null;
   *    <li>The encoded byte array form of the key is a zero length one.</ul>
   */
  private synchronized void makeKey( final byte[] key ) throws IllegalArgumentException
  {
    final byte[] kk = key;
    if( kk == null )
    {
      throw new IllegalArgumentException( "Null Blowfish key" );
    }

    int len = kk.length;
    if( len == 0 )
    {
      throw new IllegalArgumentException( "Invalid Blowfish user key length" );
    }

    if( len > MessageCipher.MAX_USER_KEY_LENGTH )
    {
      len = MessageCipher.MAX_USER_KEY_LENGTH;
    }

    // use the user-key data to generate an initial set of session key, and 
    // copy the initial s-boxes values to this sessions set
    // (one large array: sKey)
    System.arraycopy( MessageCipher.S0, 0, sKey, 0, 256 );
    System.arraycopy( MessageCipher.S1, 0, sKey, 256, 256 );
    System.arraycopy( MessageCipher.S2, 0, sKey, 512, 256 );
    System.arraycopy( MessageCipher.S3, 0, sKey, 768, 256 );

    int ri;
    for( int i = 0, j = 0; i < MessageCipher.ROUNDS + 2; i++ )
    {
      ri = 0;
      for( int k = 0; k < 4; k++ )
      {
        ri = ( ri << 8 ) | ( kk[j++] & 0xFF );
        j %= len;
      }
      P[i] = MessageCipher.Pi[i] ^ ri;
    }

    // use the former to effectively generate the session key
    makeSessionKey( 0, 0, P, 0 );
    for( int i = 2; i < MessageCipher.ROUNDS + 2; i += 2 )
    {
      makeSessionKey( P[i - 2], P[i - 1], P, i );
    }

    // and this session s-boxes
    makeSessionKey( P[MessageCipher.ROUNDS], P[MessageCipher.ROUNDS + 1], sKey, 0 );
    for( int i = 2; i < 4 * 256; i += 2 )
    {
      makeSessionKey( sKey[i - 2], sKey[i - 1], sKey, i );
    }
  }




  /**
   * @see net.bralyn.security.Cipher#getBlockSize()
   */
  public int getBlockSize()
  {
    return MessageCipher.BLOCK_SIZE;
  }




  /**
   * Quick demonstration of the blowfish cipher
   * 
   * @param args
   */
  public static void main( final String[] args )
  {
    final MessageCipher cipher = new MessageCipher();
    cipher.init( "3657".getBytes() );

    final String text = "This is a test of the Blowfish encryption algorithm at " + new Date().toString();

    byte[] bytes = text.getBytes();

    /*
     * <p>First the data is padded to blocks of data using a PKCS5 DES CBC 
     * encryption padding scheme described in section 1.1 of RFC-1423.</p>
     * 
     * <p>The last byte of the stream is ALWAYS the number of bytes added to the 
     * end of the data. If the data ends on a boundary, then there will be eight
     * bytes of padding:<code><pre>
     * 88888888 - all of the last block is padding.
     * X7777777 - the last seven bytes are padding.
     * XX666666 - etc.
     * XXX55555 - etc.
     * XXXX4444 - etc.
     * XXXXX333 - etc.
     * XXXXXX22 - etc.
     * XXXXXXX1 - only the last byte is padding.</pre></code></p>
     * 
     * <p>According to RFC1423 section 1.1:<blockquote>The input to the DES CBC encryption 
     * process shall be padded to a multiple of 8 octets, in the following 
     * manner.  Let n be the length in octets of the input. Pad the input by 
     * appending 8-(n mod 8) octets to the end of the message, each having the 
     * value 8-(n mod 8), the number of octets being added. In hexadecimal, the 
     * possible paddings are:  01, 0202, 030303, 04040404, 0505050505, 
     * 060606060606, 07070707070707, and 0808080808080808. All input is padded 
     * with 1 to 8 octets to produce a multiple of 8 octets in length. The 
     * padding can be removed unambiguously after decryption.</blockquote></p>
     */
    System.out.println( "Data length: " + bytes.length );
    System.out.println( "Modulo[" + MessageCipher.BLOCK_SIZE + "]: " + ( bytes.length % MessageCipher.BLOCK_SIZE ) );

    // pad the data as necessary using a PKCS5 (or RFC1423) padding scheme
    int padding = cipher.getBlockSize() - ( bytes.length % cipher.getBlockSize() );

    if( padding == 0 )
    {
      padding = cipher.getBlockSize();
    }

    System.out.println( "encrypt padding: " + padding );

    if( padding > 0 )
    {
      final byte[] tmp = new byte[bytes.length + padding];
      System.arraycopy( bytes, 0, tmp, 0, bytes.length );
      for( int x = bytes.length; x < tmp.length; tmp[x++] = (byte)padding )
      {
        ;
      }
      bytes = tmp;
      System.out.println( "padded data:\r\n" + ByteUtil.dump( bytes ) );
    }
    /* ********************************************************************** */

    final byte[] data = cipher.encrypt( bytes );

    System.out.println( ByteUtil.dump( data ) );
    System.out.println();

    final MessageCipher cipher2 = new MessageCipher();
    cipher2.init( "3657".getBytes() );

    byte[] data2 = cipher.decrypt( data );
    System.out.println( ByteUtil.dump( data2 ) );

    /*
     * Now we remove the padding 
     */
    padding = data2[data2.length - 1];

    if( ( padding > 0 ) && ( padding < 9 ) )
    {
      final byte[] tmp = new byte[data2.length - padding];
      System.arraycopy( data2, 0, tmp, 0, tmp.length );
      data2 = tmp;
    }

    final String text2 = new String( data2 );
    System.out.println( text2 );

  }

}
