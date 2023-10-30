package simple.cafekiosk.spring.api.service.product.request;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import simple.cafekiosk.spring.domain.product.Product;
import simple.cafekiosk.spring.domain.product.ProductSellingStatus;
import simple.cafekiosk.spring.domain.product.ProductType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@RequiredArgsConstructor
public class ProductCreateServiceRequest {

    private ProductType type;
    private ProductSellingStatus sellingStatus;
    private String name;
    private int price;

    @Builder
    private ProductCreateServiceRequest(ProductType type, ProductSellingStatus sellingStatus, String name, int price) {
        this.type = type;
        this.sellingStatus = sellingStatus;
        this.name = name;
        this.price = price;
    }

    public Product toEntity(String latestProductNumber) {
        return Product.builder()
                .productNumber(latestProductNumber)
                .type(type)
                .sellingStatus(sellingStatus)
                .name(name)
                .price(price)
                .build();
    }
}
