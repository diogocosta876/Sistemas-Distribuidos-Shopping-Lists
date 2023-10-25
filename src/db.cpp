#include "db.h"

void DBShard::setField(const std::string& value) {
    field = value;
}

std::string DBShard::getField() const {
    return field;
}
