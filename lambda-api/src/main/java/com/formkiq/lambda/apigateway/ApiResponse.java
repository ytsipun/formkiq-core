/**
 *
 * FormKiQ License
 *
 * Copyright (c) 2018 FormKiQ, INC
 * 
 * This code is the property of FormKiQ, INC. In the Software Development Agreement signed by both
 * FormKiQ and your company, FormKiQ grants you a limited license to use, modify, and create
 * derivative works of this code. Please consult the Software Development Agreement for the complete
 * terms under which you may use this code.
 *
 */
package com.formkiq.lambda.apigateway;

/** API Response Object. */
public interface ApiResponse {

  /**
   * Get Next Pagination token.
   *
   * @return {@link String}
   */
  String getNext();

  /**
   * Get Prev Pagination token.
   *
   * @return {@link String}
   */
  String getPrevious();
}
