package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    // Kiểm tra trùng số bàn khi tạo mới
    boolean existsByTableNumber(String tableNumber);

    // Kiểm tra trùng số bàn khi cập nhật
    boolean existsByTableNumberAndIdNot(String tableNumber, Long id);

    // Tìm bàn theo số bàn
    Optional<DiningTable> findByTableNumber(String tableNumber);

    // Tìm bàn theo mã QR
    Optional<DiningTable> findByTableCode(String tableCode);

    // Lấy danh sách bàn theo trạng thái
    List<DiningTable> findByStatus(DiningTable.TableStatus status);

    // Lấy toàn bộ bàn sắp xếp theo số bàn
    List<DiningTable> findAllByOrderByTableNumberAsc();
}
