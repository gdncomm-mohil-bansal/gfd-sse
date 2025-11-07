package com.gfd_sse.dummyoff2onredisreplica.dto.response;

import java.math.BigDecimal;
import java.util.List;


import com.gfd_sse.dummyoff2onredisreplica.model.CartItem;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartResponse {
  private boolean success;
  private String message;
  private String userId;
  private List<CartItem> cartItems;
  private BigDecimal totalAmount;
  private Integer totalItems;
}
