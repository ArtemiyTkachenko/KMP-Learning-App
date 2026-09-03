package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.migration.Migration
import androidx.sqlite.async.executeSQL

internal val MIGRATION_1_2 = Migration(
    startVersion = 1,
    endVersion = 2,
) { connection ->
    connection.executeSQL(
        """
        CREATE TABLE IF NOT EXISTS `test_attempt` (
            `id` TEXT NOT NULL,
            `config_type` TEXT NOT NULL,
            `requested_question_count` INTEGER NOT NULL,
            `scope_type` TEXT,
            `scope_id` TEXT,
            `status` TEXT NOT NULL,
            `score_total_questions` INTEGER,
            `score_correct_answers` INTEGER,
            `started_at_epoch_millis` INTEGER NOT NULL,
            `completed_at_epoch_millis` INTEGER,
            PRIMARY KEY(`id`)
        )
        """,
    )
    connection.executeSQL(
        """
        CREATE TABLE IF NOT EXISTS `question_attempt` (
            `test_attempt_id` TEXT NOT NULL,
            `question_id` TEXT NOT NULL,
            `sort_order` INTEGER NOT NULL,
            `is_correct` INTEGER,
            PRIMARY KEY(`test_attempt_id`, `question_id`),
            FOREIGN KEY(`test_attempt_id`) REFERENCES `test_attempt`(`id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`question_id`) REFERENCES `question`(`id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """,
    )
    connection.executeSQL(
        """
        CREATE INDEX IF NOT EXISTS `index_question_attempt_question_id`
        ON `question_attempt` (`question_id`)
        """,
    )
    connection.executeSQL(
        """
        CREATE TABLE IF NOT EXISTS `question_attempt_selected_answer` (
            `test_attempt_id` TEXT NOT NULL,
            `question_id` TEXT NOT NULL,
            `answer_id` TEXT NOT NULL,
            PRIMARY KEY(`test_attempt_id`, `question_id`, `answer_id`),
            FOREIGN KEY(`test_attempt_id`, `question_id`)
                REFERENCES `question_attempt`(`test_attempt_id`, `question_id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`question_id`, `answer_id`)
                REFERENCES `answer_option`(`question_id`, `id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """,
    )
    connection.executeSQL(
        """
        CREATE INDEX IF NOT EXISTS `index_question_attempt_selected_answer_question_id_answer_id`
        ON `question_attempt_selected_answer` (`question_id`, `answer_id`)
        """,
    )
}

/**
 * Adds the answer-option lifecycle column.
 *
 * Every option stored before this version was authored by the bundled curriculum and
 * therefore live, so existing rows default to ACTIVE. From this version on, an option
 * the curriculum stops authoring is marked DEPRECATED when a historical attempt still
 * references it, which keeps it out of new assessments while leaving it resolvable for
 * review.
 */
internal val MIGRATION_2_3 = Migration(
    startVersion = 2,
    endVersion = 3,
) { connection ->
    connection.executeSQL(
        "ALTER TABLE `answer_option` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'",
    )
}

/**
 * Preserves the interaction shown for legacy questions that had no authored mode.
 * Current bundled content subsequently replaces this derived legacy value on import.
 */
internal val MIGRATION_3_4 = Migration(
    startVersion = 3,
    endVersion = 4,
) { connection ->
    connection.executeSQL(
        "ALTER TABLE `question` ADD COLUMN `selection_mode` TEXT NOT NULL DEFAULT 'SINGLE'",
    )
    connection.executeSQL(
        """
        UPDATE `question`
        SET `selection_mode` = 'MULTIPLE'
        WHERE `id` IN (
            SELECT `question_id`
            FROM `question_correct_answer`
            GROUP BY `question_id`
            HAVING COUNT(*) > 1
        )
        """,
    )
}

/**
 * Gives v4 questions a deterministic legacy level when no authored value exists.
 * Current bundled content subsequently overwrites this value during normal import.
 */
internal val MIGRATION_4_5 = Migration(
    startVersion = 4,
    endVersion = 5,
) { connection ->
    connection.executeSQL(
        "ALTER TABLE `question` ADD COLUMN `level` TEXT NOT NULL DEFAULT 'FOUNDATION'",
    )
}

/**
 * Records which levels and which question source an attempt was practised with.
 *
 * Both columns are nullable and default to NULL, which is what makes this a pure add: every
 * existing row keeps the only selection it could have had. A FOCUSED row with NULL means the
 * all-levels ALL request that targeted practice did not yet exist to narrow, and a MIXED row has
 * no level or source dimension at all, so it keeps writing NULL from here on. Backfilling the
 * FOCUSED rows with literal level names would have claimed the learner chose something they were
 * never offered.
 */
internal val MIGRATION_5_6 = Migration(
    startVersion = 5,
    endVersion = 6,
) { connection ->
    connection.executeSQL("ALTER TABLE `test_attempt` ADD COLUMN `practice_levels` TEXT")
    connection.executeSQL("ALTER TABLE `test_attempt` ADD COLUMN `practice_source` TEXT")
}

/** Adds learner-owned saved identity without coupling it to curriculum row lifetime. */
internal val MIGRATION_6_7 = Migration(
    startVersion = 6,
    endVersion = 7,
) { connection ->
    connection.executeSQL(
        """
        CREATE TABLE IF NOT EXISTS `saved_question` (
            `question_id` TEXT NOT NULL,
            `saved_at_epoch_millis` INTEGER NOT NULL,
            PRIMARY KEY(`question_id`)
        )
        """,
    )
}
