/*
  $Id: $

  Copyright (C) 2013 Virginia Tech.
  All rights reserved.

  SEE LICENSE FOR MORE INFORMATION

  Author:  Middleware Services
  Email:   middleware@vt.edu
  Version: $Revision: $
  Updated: $Date: $
*/
package org.ldaptive.samples;

/**
 * Executes one or more samples by name.
 *
 * @author Middleware Services
 * @version $Revision: $
 */
public class SampleRunner
{
  public static void main(final String[] args)
  {
    if (args.length < 1) {
      System.out.println("USAGE: SampleRunner <SampleName> (<SampleName2> .... <SampleNameN>)");
      return;
    }
    for (int i = 0; i < args.length; i++) {
      try {
        final Sample sample = (Sample) Class.forName(args[0]).newInstance();
        sample.execute();
      } catch (InstantiationException e) {
        System.err.println("Cannot create sample " + args[0]);
      } catch (IllegalAccessException e) {
        System.err.println("Cannot create sample " + args[0]);
      } catch (ClassNotFoundException e) {
        System.err.println("Unknown sample " + args[0]);
      } catch (Exception e) {
        System.err.println("Error excecuting " + args[0]);
        e.printStackTrace();
      }
    }
  }
}
