
/*
 * Gary Cornell and Cay S. Horstmann, Core Java (Book/CD-ROM)
 * Published By SunSoft Press/Prentice-Hall
 * Copyright (C) 1996 Sun Microsystems Inc.
 * All Rights Reserved. ISBN 0-13-596891-7
 *
 * Permission to use, copy, modify, and distribute this 
 * software and its documentation for NON-COMMERCIAL purposes
 * and without fee is hereby granted provided that this 
 * copyright notice appears in all copies. 
 * 
 * THE AUTHORS AND PUBLISHER MAKE NO REPRESENTATIONS OR 
 * WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. THE AUTHORS
 * AND PUBLISHER SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED 
 * BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING 
 * THIS SOFTWARE OR ITS DERIVATIVES.
 */
 
/**
 * A class for formatting numbers that follows printf conventions.
 * Also implements C-like atoi and atof functions
 * @version 1.01 15 Feb 1996 
 * @author Cay Horstmann
 */

package coc.agent.engine;

import java.io.*;

class Format
{ /**
   * Formats the number following printf conventions.
   * Main limitation: Can only handle one format parameter at a time
   * Use multiple Format objects to format more than one number
   * @param s the format string following printf conventions
   * The string has a prefix, a format code and a suffix. The prefix and suffix
   * become part of the formatted output. The format code directs the
   * formatting of the (single) parameter to be formatted. The code has the
   * following structure
   * <ul>
   * <li> a % (required)
   * <li> a modifier (optional)
   * <dl>
   * <dt> + <dd> forces display of + for positive numbers
   * <dt> 0 <dd> show leading zeroes
   * <dt> - <dd> align left in the field
   * <dt> space <dd> prepend a space in front of positive numbers
   * <dt> # <dd> use "alternate" format. Add 0 or 0x for octal or hexadecimal numbers. Don't suppress trailing zeroes in general floating point format.
   * </dl>
   * <li> an integer denoting field width (optional)
   * <li> a period followed by an integer denoting precision (optional)
   * <li> a format descriptor (required)
   * <dl>
   * <dt>f <dd> floating point number in fixed format
   * <dt>e, E <dd> floating point number in exponential notation (scientific format). The E format results in an uppercase E for the exponent (1.14130E+003), the e format in a lowercase e.
   * <dt>g, G <dd> floating point number in general format (fixed format for small numbers, exponential format for large numbers). Trailing zeroes are suppressed. The G format results in an uppercase E for the exponent (if any), the g format in a lowercase e.
   * <dt>d, i <dd> integer in decimal
   * <dt>x <dd> integer in hexadecimal
   * <dt>o <dd> integer in octal
   * <dt>s <dd> string
   * <dt>c <dd> character
   * </dl>
   * </ul>
   * @exception ReteException 
   * @exception IllegalArgumentException <<Invalid>> if bad format
   */
  public Format(String s) throws ReteException
   {  width = 0;
      precision = -1;
      pre = "";
      post = "";
      leading_zeroes = false;
      show_plus = false;
      alternate = false;
      show_space = false;
      left_align = false;
      fmt = ' '; 
      
      int length = s.length();
      int parse_state = 0; 
      // 0 = prefix, 1 = flags, 2 = width, 3 = precision,
      // 4 = format, 5 = end
      int i = 0;
      
      while (parse_state == 0)
      {  if (i >= length) parse_state = 5;
         else if (s.charAt(i) == '%')
         {  if (i < length - 1)
            {  if (s.charAt(i + 1) == '%')
               {  pre = pre + '%';
                  i++;
               }
               else
                  parse_state = 1;
            }
            else throw new ReteException("Format.<init>", "Invalid format", s);
         }
         else
            pre = pre + s.charAt(i);
         i++;
      }
      while (parse_state == 1)
      {  if (i >= length) parse_state = 5;
         else if (s.charAt(i) == ' ') show_space = true;
         else if (s.charAt(i) == '-') left_align = true; 
         else if (s.charAt(i) == '+') show_plus = true;
         else if (s.charAt(i) == '0') leading_zeroes = true;
         else if (s.charAt(i) == '#') alternate = true;
         else { parse_state = 2; i--; }
         i++;
      }      
      while (parse_state == 2)
      {  if (i >= length) parse_state = 5;
         else if ('0' <= s.charAt(i) && s.charAt(i) <= '9')
         {  width = width * 10 + s.charAt(i) - '0';
            i++;
         }
         else if (s.charAt(i) == '.')
         {  parse_state = 3;
            precision = 0;
            i++;
         }
         else 
            parse_state = 4;            
      }
      while (parse_state == 3)
      {  if (i >= length) parse_state = 5;
         else if ('0' <= s.charAt(i) && s.charAt(i) <= '9')
         {  precision = precision * 10 + s.charAt(i) - '0';
            i++;
         }
         else 
            parse_state = 4;                  
      }
      if (parse_state == 4) 
      {  if (i >= length) parse_state = 5;
         else fmt = s.charAt(i);
         i++;
      }
      if (i < length)
         post = s.substring(i, length);
   }      

   /** 
   * Formats a double into a string (like sprintf in C)
   * @param x the number to format
   * @return the formatted string 
   * @exception IllegalArgumentException if bad argument
   */
   
   public String form(double x)
   {  String r;
      if (precision < 0) precision = 6;
      int s = 1;
      if (x < 0) { x = -x; s = -1; }
      if (fmt == 'f')
         r = fixed_format(x);
      else if (fmt == 'e' || fmt == 'E' || fmt == 'g' || fmt == 'G')
         r = exp_format(x);
      else throw new java.lang.IllegalArgumentException();
      
      return pad(sign(s, r));
   }
   
   /** 
   * Formats a long integer into a string (like sprintf in C)
   * @param x the number to format
   * @return the formatted string 
   */
   
   public String form(long x)
   {  String r; 
      int s = 0;
      if (fmt == 'd' || fmt == 'i')
      {  s = 1;
         if (x < 0) { x = -x; s = -1; }
         r = "" + x;
      }
      else if (fmt == 'o')
         r = convert(x, 3, 7, "01234567");
      else if (fmt == 'x')
         r = convert(x, 4, 15, "0123456789abcdef");
      else if (fmt == 'X')
         r = convert(x, 4, 15, "0123456789ABCDEF");
      else throw new java.lang.IllegalArgumentException();
         
      return pad(sign(s, r));
   }
   
   /**
   * Formats a character into a string (like sprintf in C)
   * @param c 
   * @param x <<Invalid>> the value to format
   * @return the formatted string
   */
  public String form(char c)
   {  if (fmt != 'c')
         throw new java.lang.IllegalArgumentException();

      String r = "" + c;
      return pad(r);
   }
   
   /**
   * Formats a string into a larger string (like sprintf in C)
   * @param s 
   * @param x <<Invalid>> the value to format
   * @return the formatted string
   */
  public String form(String s)
   {  if (fmt != 's')
         throw new java.lang.IllegalArgumentException();
      if (precision >= 0) s = s.substring(0, precision);
      return pad(s);
   }
      
   /**
   * @param c 
   * @param n 
   * @return 
   */
  private static String repeat(char c, int n)
   {  if (n <= 0) return "";
      StringBuffer s = new StringBuffer(n);
      for (int i = 0; i < n; i++) s.append(c);
      return s.toString();
   }

   /**
   * @param x 
   * @param n 
   * @param m 
   * @param d 
   * @return 
   */
  private static String convert(long x, int n, int m, String d)
   {  if (x == 0) return "0";
      String r = "";
      while (x != 0)
      {  r = d.charAt((int)(x & m)) + r;
         x = x >>> n;
      }
      return r;
   }

   /**
   * @param r 
   * @return 
   */
  private String pad(String r)
   {  String p = repeat(' ', width - r.length());
      if (left_align) return pre + r + p + post;
      else return pre + p + r + post;
   }
   
   /**
   * @param s 
   * @param r 
   * @return 
   */
  private String sign(int s, String r)
   {  String p = "";
      if (s < 0) p = "-"; 
      else if (s > 0)
      {  if (show_plus) p = "+";
         else if (show_space) p = " ";
      }
      else
      {  if (fmt == 'o' && alternate && r.length() > 0 && r.charAt(0) != '0') p = "0";
         else if (fmt == 'x' && alternate) p = "0x";
         else if (fmt == 'X' && alternate) p = "0X";
      }
      int w = 0;
      if (leading_zeroes) 
         w = width;
      else if ((fmt == 'd' || fmt == 'i' || fmt == 'x' || fmt == 'X' || fmt == 'o') 
         && precision > 0) w = precision;
      
      return p + repeat('0', w - p.length() - r.length()) + r;
   }
   
           
   /**
   * @param d 
   * @return 
   */
  private String fixed_format(double d)
   {  String f = "";

      if (d > 0x7FFFFFFFFFFFFFFFL) return exp_format(d);
   
      long l = (long)(precision == 0 ? d + 0.5 : d);
      f = f + l;
      
      double fr = d - l; // fractional part
      if (fr >= 1 || fr < 0) return exp_format(d);
    
      return f + frac_part(fr);
   }   
   
   /**
   * @param fr 
   * @return 
   */
  private String frac_part(double fr)
   /**
   * precondition: 0 <= fr < 1
   * @param d 
   * @return 
   */
  {  String z = "";
      if (precision > 0)
      {  double factor = 1;
         String leading_zeroes = "";
         for (int i = 1; i <= precision && factor <= 0x7FFFFFFFFFFFFFFFL; i++) 
         {  factor *= 10; 
            leading_zeroes = leading_zeroes + "0"; 
         }
         long l = (long) (factor * fr + 0.5);

         z = leading_zeroes + l;
         z = z.substring(z.length() - precision, z.length());
      }

      
      if (precision > 0 || alternate) z = "." + z;
      if ((fmt == 'G' || fmt == 'g') && !alternate)
      // remove trailing zeroes and decimal point
      {  int t = z.length() - 1;
         while (t >= 0 && z.charAt(t) == '0') t--;
         if (t >= 0 && z.charAt(t) == '.') t--;
         z = z.substring(0, t + 1);
      }
      return z;
   }

   private String exp_format(double d)
   {  String f = "";
      int e = 0;
      double dd = d;
      double factor = 1;
      while (dd > 10) { e++; factor /= 10; dd = dd / 10; }
      while (dd < 1) { e--; factor *= 10; dd = dd * 10; }
      if ((fmt == 'g' || fmt == 'G') && e >= -4 && e < precision) 
         return fixed_format(d);
      
      d = d * factor;
      f = f + fixed_format(d);
      
      if (fmt == 'e' || fmt == 'g')
         f = f + "e";
      else
         f = f + "E";

      String p = "000";      
      if (e >= 0) 
      {  f = f + "+";
         p = p + e;
      }
      else
      {  f = f + "-";
         p = p + (-e);
      }
         
      return f + p.substring(p.length() - 3, p.length());
   }
   
   private int width;
   private int precision;
   private String pre;
   private String post;
   private boolean leading_zeroes;
   private boolean show_plus;
   private boolean alternate;
   private boolean show_space;
   private boolean left_align;
   char fmt; // one of cdeEfgGiosxXos
  
}
