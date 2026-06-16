package com.nicolas.familybudget.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun accountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun txType(v: TransactionType): String = v.name
    @TypeConverter fun toTxType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun catKind(v: CategoryKind): String = v.name
    @TypeConverter fun toCatKind(v: String): CategoryKind = CategoryKind.valueOf(v)

    @TypeConverter fun bucket(v: BudgetBucket): String = v.name
    @TypeConverter fun toBucket(v: String): BudgetBucket = BudgetBucket.valueOf(v)

    @TypeConverter fun txSource(v: TransactionSource): String = v.name
    @TypeConverter fun toTxSource(v: String): TransactionSource = TransactionSource.valueOf(v)

    @TypeConverter fun goalType(v: GoalType): String = v.name
    @TypeConverter fun toGoalType(v: String): GoalType = GoalType.valueOf(v)

    @TypeConverter fun role(v: MemberRole): String = v.name
    @TypeConverter fun toRole(v: String): MemberRole = MemberRole.valueOf(v)
}
