package com.qros.modules.table.mapper;

import com.qros.modules.table.dto.response.TableServiceRequestResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.TableServiceRequest;
import com.qros.modules.table.model.TableSession;
import com.qros.modules.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class TableServiceRequestMapper {

    public TableServiceRequestResponse toResponse(TableServiceRequest request, String tableDisplayName) {
        if (request == null) {
            return null;
        }

        DiningTable table = request.getTable();
        TableSession session = request.getTableSession();
        User claimedBy = request.getClaimedByUser();
        User resolvedBy = request.getResolvedByUser();

        String resolvedDisplayName = tableDisplayName != null && !tableDisplayName.isBlank()
                ? tableDisplayName
                : table != null && table.getTableNumber() != null ? "Table " + table.getTableNumber() : "Table";

        return new TableServiceRequestResponse(
                request.getId(),
                table != null ? table.getId() : null,
                table != null ? table.getTableCode() : null,
                table != null ? table.getTableNumber() : null,
                resolvedDisplayName,
                session != null ? session.getId() : null,
                request.getReason(),
                request.getNote(),
                request.getStatus(),
                claimedBy != null ? claimedBy.getId() : null,
                claimedBy != null ? claimedBy.getFullName() : null,
                request.getClaimedAt(),
                resolvedBy != null ? resolvedBy.getId() : null,
                resolvedBy != null ? resolvedBy.getFullName() : null,
                request.getResolvedAt(),
                request.getCancelledByType(),
                request.getCancelledReason(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getVersion());
    }
}
