// SPDX-License-Identifier: MIT
pragma solidity ^0.8.30;
/// @notice Digest-only audit anchors. No funds, wallets or personal records are stored.
contract AuditAnchor {
    address public immutable writer;
    mapping(bytes32 => bytes32) public records;
    event Anchored(bytes32 indexed recordId, bytes32 digest);
    error Unauthorized();
    error EmptyRecord();
    error DigestConflict();
    constructor(address authorizedWriter) {
        if (authorizedWriter == address(0)) revert EmptyRecord();
        writer = authorizedWriter;
    }
    function anchor(bytes32 recordId, bytes32 digest) external {
        if (msg.sender != writer) revert Unauthorized();
        if (recordId == bytes32(0) || digest == bytes32(0)) revert EmptyRecord();
        bytes32 previous = records[recordId];
        if (previous == digest) return; // Safe at-least-once delivery.
        if (previous != bytes32(0)) revert DigestConflict();
        records[recordId] = digest;
        emit Anchored(recordId, digest);
    }
}
