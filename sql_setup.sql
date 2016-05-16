
/**
 * for 6x6 table
 * the fields table should also be partitioned
 * tis would require the primary to be on all fields, creating even bigger
 * disk requirements for it's storage
 */

CREATE TABLE `moves_6_6` (
 `fid` int(10) unsigned NOT NULL,
 `player_a` bit(1) NOT NULL,
 `move` tinyint(4) NOT NULL,
 `used` bit(1) NOT NULL,
 `loose` bit(1) NOT NULL,
 `draw` bit(1) NOT NULL,
 `win` bit(1) NOT NULL,
 PRIMARY KEY (`fid`,`player_a`,`move`) USING BTREE,
 KEY `player_a` (`player_a`),
 KEY `fid` (`fid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='moves table, partitioned'
/*!50100 PARTITION BY RANGE (`fid`)
(PARTITION p0 VALUES LESS THAN (25000000) ENGINE = InnoDB,
PARTITION p1 VALUES LESS THAN (50000000) ENGINE = InnoDB,
PARTITION p2 VALUES LESS THAN (75000000) ENGINE = InnoDB,
PARTITION p3 VALUES LESS THAN (100000000) ENGINE = InnoDB,
PARTITION p4 VALUES LESS THAN (125000000) ENGINE = InnoDB,
PARTITION p5 VALUES LESS THAN (150000000) ENGINE = InnoDB,
PARTITION p6 VALUES LESS THAN (175000000) ENGINE = InnoDB,
PARTITION p7 VALUES LESS THAN (200000000) ENGINE = InnoDB,
PARTITION p8 VALUES LESS THAN MAXVALUE ENGINE = InnoDB) */

CREATE TABLE `fields` (
 `field` binary(20) NOT NULL,
 `fid` int(10) unsigned NOT NULL AUTO_INCREMENT,
 PRIMARY KEY (`fid`),
 UNIQUE KEY `field` (`field`)
) ENGINE=InnoDB AUTO_INCREMENT=4467702 DEFAULT CHARSET=utf8