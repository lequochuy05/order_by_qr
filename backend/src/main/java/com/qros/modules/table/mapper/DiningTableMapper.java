package com.qros.modules.table.mapper;

import com.qros.modules.menu.dto.publicmenu.PublicTable;
import com.qros.modules.table.dto.response.DiningTableResponse;
import com.qros.modules.table.model.DiningTable;
import org.springframework.stereotype.Component;

@Component
public class DiningTableMapper {

    public DiningTableResponse toResponse(DiningTable table) {
        if (table == null) {
            return null;
        }

        return new DiningTableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getTableCode(),
                table.getStatus(),
                table.getCapacity(),
                table.getQrCodeUrl(),
                table.getCreatedAt(),
                table.getUpdatedAt()
        );
    }

    public PublicTable toPublicTable(DiningTable table) {
        if (table == null) {
            return null;
        }

        return new PublicTable(
                table.getId(),
                table.getTableNumber()
        );
    }
}