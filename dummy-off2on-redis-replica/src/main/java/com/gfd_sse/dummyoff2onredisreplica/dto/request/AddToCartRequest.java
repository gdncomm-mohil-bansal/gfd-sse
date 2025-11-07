package com.gfd_sse.dummyoff2onredisreplica.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
  private String userId;
  private Long productId;
  private Integer quantity;
}

