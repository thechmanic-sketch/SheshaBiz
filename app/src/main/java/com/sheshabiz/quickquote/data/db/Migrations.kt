package com.sheshabiz.quickquote.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: added the Invoice / InvoiceItem tables. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `invoices` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `invoiceNumber` TEXT NOT NULL,
                `sourceQuoteId` INTEGER,
                `customerId` INTEGER,
                `customerName` TEXT NOT NULL,
                `customerPhone` TEXT NOT NULL,
                `customerEmail` TEXT,
                `customerAddress` TEXT,
                `invoiceDate` INTEGER NOT NULL,
                `dueDate` INTEGER NOT NULL,
                `vatEnabled` INTEGER NOT NULL,
                `vatRate` REAL NOT NULL,
                `discountType` TEXT NOT NULL,
                `discountValue` REAL NOT NULL,
                `subtotal` REAL NOT NULL,
                `discountAmount` REAL NOT NULL,
                `vatAmount` REAL NOT NULL,
                `total` REAL NOT NULL,
                `notes` TEXT,
                `paymentTerms` TEXT,
                `status` TEXT NOT NULL,
                `paidAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`sourceQuoteId`) REFERENCES `quotes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_customerId` ON `invoices` (`customerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_sourceQuoteId` ON `invoices` (`sourceQuoteId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_invoiceNumber` ON `invoices` (`invoiceNumber`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `invoice_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `invoiceId` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unitPrice` REAL NOT NULL,
                `lineTotal` REAL NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_items_invoiceId` ON `invoice_items` (`invoiceId`)")
    }
}

/** v2 -> v3: business profile reg number + banking fields, and the Product / Sale / SaleItem tables. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `registrationNumber` TEXT")
        db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `bankName` TEXT")
        db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `accountHolder` TEXT")
        db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `accountNumber` TEXT")
        db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `branchCode` TEXT")
        db.execSQL("ALTER TABLE `business_profile` ADD COLUMN `accountType` TEXT")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `products` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `unitPrice` REAL NOT NULL,
                `sku` TEXT,
                `trackStock` INTEGER NOT NULL,
                `stockQuantity` REAL NOT NULL,
                `lowStockThreshold` REAL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sales` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `saleNumber` TEXT NOT NULL,
                `customerId` INTEGER,
                `customerName` TEXT NOT NULL,
                `saleDate` INTEGER NOT NULL,
                `vatEnabled` INTEGER NOT NULL,
                `vatRate` REAL NOT NULL,
                `discountType` TEXT NOT NULL,
                `discountValue` REAL NOT NULL,
                `subtotal` REAL NOT NULL,
                `discountAmount` REAL NOT NULL,
                `vatAmount` REAL NOT NULL,
                `total` REAL NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `notes` TEXT,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sales_saleNumber` ON `sales` (`saleNumber`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sale_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `saleId` INTEGER NOT NULL,
                `productId` INTEGER,
                `description` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `unitPrice` REAL NOT NULL,
                `lineTotal` REAL NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
    }
}

/** v3 -> v4: added an optional product photo. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `products` ADD COLUMN `imageUri` TEXT")
    }
}

/** v4 -> v5: cash tendered/change on a sale, shown on the receipt. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `sales` ADD COLUMN `amountTendered` REAL")
        db.execSQL("ALTER TABLE `sales` ADD COLUMN `changeGiven` REAL")
    }
}

/**
 * v5 -> v6: cross-device sync bookkeeping. Adds `syncId` (this row's stable remote uuid,
 * assigned lazily on first push) and `deletedAt` (a soft-delete tombstone timestamp, epoch
 * millis) to every synced table, plus a partial unique index on `syncId` so a Supabase
 * `Prefer: resolution=merge-duplicates` upsert can key off it. `customers` also gains
 * `updatedAt` — the only synced entity that both (a) can be edited after creation and
 * (b) had no existing modification clock for last-write-wins conflict resolution; it
 * defaults to 0 so pre-existing rows simply lose every last-write-wins tie until their
 * first real edit or sync pull.
 *
 * Purely additive (`ADD COLUMN` + `CREATE INDEX`, same shape as every migration above) —
 * no table is dropped or recreated, so no existing quotes/invoices/customers/products/sales
 * are touched. See commit c5d1cb6 ("Fix: stop destroying user data on every schema-version
 * update") for the destructive-fallback bug this deliberately avoids repeating.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val syncedTables = listOf(
            "business_profile", "customers", "products",
            "quotes", "invoices", "sales",
            "quote_items", "invoice_items", "sale_items"
        )
        for (table in syncedTables) {
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncId` TEXT")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `deletedAt` INTEGER")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_syncId` " +
                    "ON `$table` (`syncId`) WHERE `syncId` IS NOT NULL"
            )
        }
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
    }
}
