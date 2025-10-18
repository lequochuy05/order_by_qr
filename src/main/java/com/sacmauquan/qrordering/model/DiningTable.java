package com.sacmauquan.qrordering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tables")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_number", nullable = false, unique = true, length = 20)
    private String tableNumber;

    @Column(name = "qr_code_url", length = 255)
    private String qrCodeUrl; // ảnh QR công khai hiển thị cho khách

    @Column(name = "qr_code_public_id", length = 255)
    private String qrCodePublicId; // mã Cloudinary để xóa ảnh khi cần

    @Column(name = "table_code", nullable = false, unique = true, length = 50)
    private String tableCode; // mã ngẫu nhiên bảo mật cho QR (vd: weirrierffir)

    private String status;
    private int capacity;
}
