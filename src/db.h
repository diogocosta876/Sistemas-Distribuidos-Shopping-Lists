#pragma once
#include <string>

class DBShard
{
public:
    void setField(const std::string &value);
    std::string getField() const;

private:
    std::string field;
};